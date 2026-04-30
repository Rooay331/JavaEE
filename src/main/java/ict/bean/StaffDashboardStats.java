package ict.bean;

import java.io.Serializable;

public class StaffDashboardStats implements Serializable {

    private int todayAppointments;
    private int waitingQueueTickets;
    private int pendingApprovals;
    private int openServiceIssues;

    public StaffDashboardStats() {
    }

    public StaffDashboardStats(int todayAppointments, int waitingQueueTickets, int pendingApprovals, int openServiceIssues) {
        this.todayAppointments = todayAppointments;
        this.waitingQueueTickets = waitingQueueTickets;
        this.pendingApprovals = pendingApprovals;
        this.openServiceIssues = openServiceIssues;
    }

    public int getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(int todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public int getWaitingQueueTickets() {
        return waitingQueueTickets;
    }

    public void setWaitingQueueTickets(int waitingQueueTickets) {
        this.waitingQueueTickets = waitingQueueTickets;
    }

    public int getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(int pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    public int getOpenServiceIssues() {
        return openServiceIssues;
    }

    public void setOpenServiceIssues(int openServiceIssues) {
        this.openServiceIssues = openServiceIssues;
    }
}