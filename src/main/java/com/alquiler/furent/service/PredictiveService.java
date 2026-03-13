package com.alquiler.furent.service;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de analítica predictiva sencilla para demanda de mobiliario.
 * Calcula la cantidad total de unidades reservadas por día (sumando items)
 * y aplica una media móvil para proyectar la demanda futura.
 */
@Service
public class PredictiveService {

    private final ReservationRepository reservationRepository;

    public PredictiveService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Genera una serie histórica y proyecciones para:
     * - unidades (suma de cantidades de todos los items)
     * - ingresos (suma del total de las reservas)
     * - reservas (cantidad de reservas)
     */
    public Map<String, Object> generateForecasts(int historyDays, int forecastDays) {
        if (historyDays <= 0) historyDays = 60;
        if (forecastDays <= 0) forecastDays = 14;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(historyDays - 1L);

        List<Reservation> todasReservas = reservationRepository.findAll();

        Map<LocalDate, BigDecimal> agregadosUnidades = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> agregadosIngresos = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> agregadosReservas = new LinkedHashMap<>();

        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            agregadosUnidades.put(d, BigDecimal.ZERO);
            agregadosIngresos.put(d, BigDecimal.ZERO);
            agregadosReservas.put(d, BigDecimal.ZERO);
        }

        for (Reservation r : todasReservas) {
            if (r.getFechaInicio() == null || r.getItems() == null || r.getItems().isEmpty()) continue;
            LocalDate dia = r.getFechaInicio();
            if (dia.isBefore(from) || dia.isAfter(today)) continue;

            // Unidades
            BigDecimal totalUnidades = r.getItems().stream()
                    .map(item -> BigDecimal.valueOf(item.getCantidad()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregadosUnidades.put(dia, agregadosUnidades.get(dia).add(totalUnidades));

            // Ingresos
            BigDecimal ingresos = r.getTotal() != null ? r.getTotal() : BigDecimal.ZERO;
            agregadosIngresos.put(dia, agregadosIngresos.get(dia).add(ingresos));

            // Reservas count
            agregadosReservas.put(dia, agregadosReservas.get(dia).add(BigDecimal.ONE));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        
        result.put("history_unidades", convertToStringMap(agregadosUnidades, historyDays, from, 0));
        result.put("forecast_unidades", createForecast(agregadosUnidades, forecastDays, today, 0));
        
        result.put("history_ingresos", convertToStringMap(agregadosIngresos, historyDays, from, 2));
        result.put("forecast_ingresos", createForecast(agregadosIngresos, forecastDays, today, 2));
        
        result.put("history_reservas", convertToStringMap(agregadosReservas, historyDays, from, 0));
        result.put("forecast_reservas", createForecast(agregadosReservas, forecastDays, today, 0));
        
        return result;
    }

    private LinkedHashMap<String, BigDecimal> convertToStringMap(Map<LocalDate, BigDecimal> agregados, int historyDays, LocalDate from, int scale) {
        LinkedHashMap<String, BigDecimal> history = new LinkedHashMap<>();
        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            BigDecimal value = agregados.getOrDefault(d, BigDecimal.ZERO);
            history.put(d.toString(), value.setScale(scale, RoundingMode.HALF_UP));
        }
        return history;
    }

    private LinkedHashMap<String, BigDecimal> createForecast(Map<LocalDate, BigDecimal> agregados, int forecastDays, LocalDate today, int scale) {
        LinkedHashMap<String, BigDecimal> forecast = new LinkedHashMap<>();
        int window = 7;
        
        // Convert to array in chronological order (assuming agregados is already ordered, but let's be safe)
        BigDecimal[] series = agregados.values().toArray(new BigDecimal[0]);

        for (int i = 0; i < forecastDays; i++) {
            int count = Math.min(window, series.length + i);
            if (count == 0) {
                forecast.put(today.plusDays(i + 1L).toString(), BigDecimal.ZERO);
                continue;
            }

            BigDecimal sum = BigDecimal.ZERO;
            for (int k = 0; k < count; k++) {
                int index = series.length + i - 1 - k;
                if (index >= 0 && index < series.length) {
                    sum = sum.add(series[index]);
                } else {
                    int forecastIndex = (series.length + i - 1) - series.length - (count - 1 - k);
                    if (forecastIndex >= 0) {
                        BigDecimal projected = forecast.values().toArray(new BigDecimal[0])[forecastIndex];
                        sum = sum.add(projected);
                    }
                }
            }

            BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            LocalDate futureDate = today.plusDays(i + 1L);
            forecast.put(futureDate.toString(), avg.max(BigDecimal.ZERO));
        }
        return forecast;
    }
}


