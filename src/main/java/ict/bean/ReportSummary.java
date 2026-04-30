package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportSummary implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");

    private LocalDate fromDate;
    private LocalDate toDate;
    private String periodLabel;
    private int dailyCompletedAppointments;
    private int dailyNoShowCount;
    private int weeklyServedQueueTickets;
    private int serviceUtilizationAverage;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public int getDailyCompletedAppointments() {
        return dailyCompletedAppointments;
    }

    public void setDailyCompletedAppointments(int dailyCompletedAppointments) {
        this.dailyCompletedAppointments = dailyCompletedAppointments;
    }

    public int getDailyNoShowCount() {
        return dailyNoShowCount;
    }

    public void setDailyNoShowCount(int dailyNoShowCount) {
        this.dailyNoShowCount = dailyNoShowCount;
    }

    public int getWeeklyServedQueueTickets() {
        return weeklyServedQueueTickets;
    }

    public void setWeeklyServedQueueTickets(int weeklyServedQueueTickets) {
        this.weeklyServedQueueTickets = weeklyServedQueueTickets;
    }

    public int getServiceUtilizationAverage() {
        return serviceUtilizationAverage;
    }

    public void setServiceUtilizationAverage(int serviceUtilizationAverage) {
        this.serviceUtilizationAverage = serviceUtilizationAverage;
    }

    public String getDateRangeLabel() {
        if (fromDate == null || toDate == null) {
            return "";
        }
        if (fromDate.equals(toDate)) {
            return fromDate.format(DATE_FORMATTER);
        }
        return fromDate.format(DATE_FORMATTER) + " - " + toDate.format(DATE_FORMATTER);
    }

    public String getServiceUtilizationLabel() {
        return serviceUtilizationAverage + "%";
    }
}