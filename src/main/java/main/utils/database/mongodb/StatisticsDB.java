package main.utils.database.mongodb;

import lombok.SneakyThrows;
import main.constants.Database;
import main.constants.Statistic;
import main.utils.json.AbstractJSON;
import main.utils.json.GenericJSONField;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.UnexpectedException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatisticsDB extends AbstractMongoDatabase implements AbstractJSON {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDB.class);

    public StatisticsDB() {
        super(Database.MONGO.ROBERTIFY_DATABASE, Database.MONGO.ROBERTIFY_STATS);
    }

    public void incrementStatistic(long increment, Statistic statistic) {
        setCurrentStatistic(getCurrentStatistic(statistic, TimeFormat.DAY) + increment, statistic);
    }

    public void setCurrentStatistic(long val, Statistic statistic) {
        final Document document = getDocument();
        final var jsonObj = new JSONObject(document.toJson());

        jsonObj.getJSONObject(Fields.CURRENT_YEAR.toString())
                .getJSONObject(Fields.CURRENT_MONTH.toString())
                .getJSONObject(Fields.CURRENT_DAY.toString())
                .getJSONObject(Fields.STATS.toString())
                .put(statistic.toString(), val);

        updateDocument(document, jsonObj);
    }

    @SneakyThrows
    public long getCurrentStatistic(Statistic stat, TimeFormat format) {
        switch (format) {
            case DAY -> {
                return getStatForDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), stat);
            }
            case WEEK -> {
                return getStatForWeek(stat);
            }
            case MONTH -> {
                return getStatForMonth(Calendar.getInstance().get(Calendar.MONTH), stat);
            }
            case YEAR -> {
                return getStatForYear(stat);
            }
        }
        throw new UnexpectedException("How did this happen?");
    }

    @SneakyThrows
    private long getStatForDay(int day, int month, Statistic stat) {
        final int curMonth = Calendar.getInstance().get(Calendar.MONTH);

        month = curMonth == month ? -1 : month;

        final var currYearStats = getFullJSON().getJSONObject(Fields.CURRENT_YEAR.toString());

        if (month < 0) { // Current month
            final var currMonth = currYearStats.getJSONObject(Fields.CURRENT_MONTH.toString());

            final int curDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            long ret = 0;
            if (curDay == day) {
                final var currDay = currMonth.getJSONObject(Fields.CURRENT_DAY.toString());
                ret += currDay.getJSONObject(Fields.STATS.toString()).getLong(stat.toString());
            } else {
                if (currMonth.has(Fields.PAST_DAYS.toString())) {
                    final var pastDays = currMonth.getJSONArray(Fields.PAST_DAYS.toString());

                    if (arrayHasObject(pastDays, Fields.DAY_OF_MONTH, day))
                        ret += pastDays.getJSONObject(getIndexOfObjectInArray(pastDays, Fields.DAY_OF_MONTH, day)).getJSONObject(Fields.STATS.toString()).getLong(stat.toString());

                }
            }
            return ret;
        } else { // Specified month
            final var pastMonths = currYearStats.getJSONArray(Fields.PAST_MONTHS.toString());
            try {
                final var monthObj = pastMonths.getJSONObject(getIndexOfObjectInArray(pastMonths, Fields.MONTH, month));

                if (!monthObj.has(String.valueOf(day))) return 0;

                return monthObj.getJSONObject(String.valueOf(day)).getJSONObject(Fields.STATS.toString()).getLong(stat.toString());
            } catch (NullPointerException e) {
                return 0;
            } catch (Exception e) {
                logger.error("An unexpected error occurred!", e);
            }
        }
        throw new UnexpectedException("How did this happen?");
    }

    public long getStatForDay(int day, Statistic stat) {
        return getStatForDay(day, -1, stat);
    }

    public long getStatForWeek(Statistic stat) {
        final var calendar = Calendar.getInstance();
        final var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        long ret = 0;
        for (int i = dayOfWeek; i <= 7; i++)
            ret += getStatForDay(i, stat);

        for (int i = dayOfWeek; i > 0; i--)
            ret += getStatForDay(i, stat);

        return  ret;
    }

    public long getStatForMonth(int month, Statistic stat) {
        long ret = 0;

        for (int i = 1; i <= 32; i++)
            ret += getStatForDay(i, month, stat);

        return ret;
    }

    public long getStatForYear(Statistic stat) {
        long ret = 0;

        for (int i = 0; i < 12; i++)
            ret += getStatForMonth(i, stat);

        return ret;
    }


    private JSONObject getFullJSON() {
        return new JSONObject(getDocument().toJson());
    }

    private Document getDocument(){
        return getCollection().find().iterator().next();
    }

    // Boring back-end stuff incoming!
    @Override
    public void init() {
        if (getCollection().countDocuments() == 0) {
            addDocument(
                    DocumentBuilder.create()
                            .addField("lastUpdated", System.currentTimeMillis())
                            .build()
            );
        }
        update();
    }

    public void update() {
        final Document document = getCollection().find().iterator().next();
        final JSONObject jsonObject = new JSONObject(document.toJson());

        if (!jsonObject.has(Fields.CURRENT_YEAR.toString())) {
            jsonObject.put(Fields.CURRENT_YEAR.toString(), initYear());
            updateDocument(document, jsonObject);
            return;
        }

        final JSONObject currentYearObj = jsonObject.getJSONObject(Fields.CURRENT_YEAR.toString());

        final var calendar = Calendar.getInstance();
        final var yearVal = calendar.get(Calendar.YEAR);
        final var monthVal = calendar.get(Calendar.MONTH);
        final var dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        if (!currentYearObj.getString(Fields.YEAR.toString()).equals(String.valueOf(yearVal))) {
            updateYear(jsonObject);
            updateDocument(document, jsonObject);
            return;
        }

        final var currentMonthObj = currentYearObj.getJSONObject(Fields.CURRENT_MONTH.toString());

        if (currentMonthObj.getInt(Fields.MONTH.toString()) != monthVal) {
            updateMonth(jsonObject);
            updateDocument(document, jsonObject);
            return;
        }

        final var currentDayObj = currentMonthObj.getJSONObject(Fields.CURRENT_DAY.toString());

        if (currentDayObj.getInt(Fields.DAY_OF_MONTH.toString()) != dayOfMonth) {
            updateDay(jsonObject);
            updateDocument(document, jsonObject);
        }
    }

    private void updateYear(JSONObject object) {
        final JSONObject currYearObj = object.getJSONObject(Fields.CURRENT_YEAR.toString());
        final JSONObject currMonthObj = currYearObj.getJSONObject(Fields.CURRENT_MONTH.toString());
        JSONArray pastMonthsArr;

        try {
            pastMonthsArr = currYearObj.getJSONArray(Fields.PAST_MONTHS.toString());
        } catch (Exception e) {
            pastMonthsArr = null;
        }

        if (!object.has(Fields.PAST_YEARS.toString()))
            object.put(Fields.PAST_YEARS.toString(), new JSONArray());

        final JSONArray pastYearsArr = object.getJSONArray(Fields.PAST_YEARS.toString());

        final JSONObject yearObj = new JSONObject();

        yearObj.put(Fields.YEAR.toString(), currYearObj.getString(Fields.YEAR.toString()));

        if (pastMonthsArr != null)
            for (int i = 0; i < pastMonthsArr.length(); i++) {
                final var monthObj = pastMonthsArr.getJSONObject(i);
                yearObj.put(String.valueOf(monthObj.getInt(Fields.MONTH.toString())), monthObj.remove(Fields.MONTH.toString()));
            }

        final JSONObject lastMonthObj = new JSONObject();
        JSONArray pastDaysArr;

        try {
            pastDaysArr = currMonthObj.getJSONArray(Fields.PAST_DAYS.toString());
        } catch (Exception e) {
            pastDaysArr = null;
        }

        if (pastDaysArr != null) {
            for (int i = 0; i < pastDaysArr.length(); i++) {
                final var dayObj = pastDaysArr.getJSONObject(i);
                lastMonthObj.put(String.valueOf(dayObj.getInt(Fields.DAY_OF_MONTH.toString())), dayObj.getJSONObject(Fields.STATS.toString()));
            }
        }

        final var currDayObj = currMonthObj.getJSONObject(Fields.CURRENT_DAY.toString());
        lastMonthObj.put(String.valueOf(currDayObj.getInt(Fields.DAY_OF_MONTH.toString())), currDayObj.getJSONObject(Fields.STATS.toString()));

        yearObj.put(String.valueOf(currMonthObj.getInt(Fields.MONTH.toString())), lastMonthObj);

        pastYearsArr.put(yearObj);
        object.remove(Fields.CURRENT_YEAR.toString());
        object.put(Fields.CURRENT_YEAR.toString(), initYear());
    }

    private void updateMonth(JSONObject object) {
        final JSONObject currYearObj = object.getJSONObject(Fields.CURRENT_YEAR.toString());
        final JSONObject currMonthObj = currYearObj.getJSONObject(Fields.CURRENT_MONTH.toString());

        if (!currYearObj.has(Fields.PAST_MONTHS.toString()))
            currYearObj.put(Fields.PAST_MONTHS.toString(), new JSONArray());

        final JSONArray pastMonthsArr = currYearObj.getJSONArray(Fields.PAST_MONTHS.toString());
        final JSONObject monthObj = new JSONObject()
                .put(Fields.MONTH.toString(), currMonthObj.getInt(Fields.MONTH.toString()));

        if (currMonthObj.has(Fields.PAST_DAYS.toString())) {
            final var pastDays = currMonthObj.getJSONArray(Fields.PAST_DAYS.toString());

            for (int i = 0; i < pastDays.length(); i++) {
                final var pastDay = pastDays.getJSONObject(i);

                monthObj.put(
                        String.valueOf(pastDay.getInt(Fields.DAY_OF_MONTH.toString())),
                        new JSONObject().put(Fields.STATS.toString(), pastDay.getJSONObject(Fields.STATS.toString()))
                );
            }
        }

        final var currentDayObj = currMonthObj.getJSONObject(Fields.CURRENT_DAY.toString());
        monthObj.put(String.valueOf(currentDayObj.getInt(Fields.DAY_OF_MONTH.toString())), new JSONObject().put(Fields.STATS.toString(), currentDayObj.getJSONObject(Fields.STATS.toString())));

        pastMonthsArr.put(monthObj);
        currYearObj.remove(Fields.CURRENT_MONTH.toString());
        currYearObj.put(Fields.CURRENT_MONTH.toString(), initMonth());
    }

    private void updateDay(JSONObject object) {
        final JSONObject currYearObj = object.getJSONObject(Fields.CURRENT_YEAR.toString());
        final JSONObject currMonthObj = currYearObj.getJSONObject(Fields.CURRENT_MONTH.toString());
        final var currDayObj = currMonthObj.getJSONObject(Fields.CURRENT_DAY.toString());

        if (!currMonthObj.has(Fields.PAST_DAYS.toString()))
            currMonthObj.put(Fields.PAST_DAYS.toString(), new JSONArray());

        final var pastDaysArr = currMonthObj.getJSONArray(Fields.PAST_DAYS.toString());
        final var pastDayObj = new JSONObject()
                .put(Fields.DAY_OF_MONTH.toString(), currDayObj.getInt(Fields.DAY_OF_MONTH.toString()))
                .put(Fields.STATS.toString(), currDayObj.getJSONObject(Fields.STATS.toString()));

        pastDaysArr.put(pastDayObj);
        currDayObj.clear();
        currMonthObj.put(Fields.CURRENT_DAY.toString(), initDay());
    }

    private JSONObject initYear() {
        final JSONObject year = new JSONObject();
        final var calendar = Calendar.getInstance();
        final var yearVal = calendar.get(Calendar.YEAR);

        year.put(Fields.YEAR.toString(), String.valueOf(yearVal));
        year.put(Fields.CURRENT_MONTH.toString(), initMonth());

        return year;
    }

    private JSONObject initMonth() {
        final JSONObject month = new JSONObject();
        final var calendar = Calendar.getInstance();
        final var monthVal = calendar.get(Calendar.MONTH);

        month.put(Fields.MONTH.toString(), monthVal);
        month.put(Fields.CURRENT_DAY.toString(), initDay());
        return month;
    }

    public JSONObject initDay() {
        final var day = new JSONObject();
        final var calendar = Calendar.getInstance();
        final var dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        final var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        day.put(Fields.DAY_OF_MONTH.toString(), dayOfMonth);
        day.put(Fields.DAY_OF_WEEK.toString(), dayOfWeek);

        final var statsObj = new JSONObject();

        for (var stats : Statistic.values())
            statsObj.put(stats.toString(), 0);

        day.put(Fields.STATS.toString(), statsObj);

        return day;
    }

    // Schedulers
    public static void startDailyUpdateCheck() {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        logger.info("Starting statistic executor service");
        executorService.scheduleAtFixedRate(
                () -> {
                    new StatisticsDB().update();
                }, 1, 1, TimeUnit.HOURS
        );
    }

    // Enums
    public enum TimeFormat {
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    public enum Fields implements GenericJSONField {
        CURRENT_YEAR("current_year"),
        YEAR("year"),
        CURRENT_MONTH("current_month"),
        MONTH("month"),
        CURRENT_DAY("current_day"),
        DAY_OF_MONTH("day_of_month"),
        DAY_OF_WEEK("day_of_week"),
        PAST_YEARS("past_years"),
        PAST_MONTHS("past_months"),
        PAST_DAYS("past_days"),
        STATS("stats");

        private final String str;

        Fields(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
