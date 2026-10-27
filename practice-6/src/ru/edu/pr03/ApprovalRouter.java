package ru.edu.pr03;

public class ApprovalRouter {
    public static final long FINANCE_LIMIT = 500_000L;

    public ApprovalRoute route(long amount, boolean urgent,
                               RiskLevel risk, boolean containsPersonalData, String department) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
        if (requiresSecurityReview(containsPersonalData)) return ApprovalRoute.SECURITY_REVIEW;
        if (requiresDirectorReview(risk)) return ApprovalRoute.DIRECTOR_REVIEW;
        if (addDepartmentPriority(department, urgent) == 1) return ApprovalRoute.FAST_TRACK;
        if (requiresFinanceReview(amount)) return ApprovalRoute.FINANCE_REVIEW;
        if (requiresFastTrack(amount, urgent)) return ApprovalRoute.FAST_TRACK;
        return ApprovalRoute.STANDARD;
    }

    public int addDepartmentPriority(String department, boolean urgent) {
        return "IT".equals(department) && urgent ? 1 : 0;
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }
}
