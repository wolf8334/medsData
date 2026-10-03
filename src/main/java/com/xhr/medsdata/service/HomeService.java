package com.xhr.medsdata.service;

import com.xhr.medsdata.common.Period;
import com.xhr.medsdata.domain.Person;
import com.xhr.medsdata.domain.Plan;
import com.xhr.medsdata.domain.TakeRecord;
import com.xhr.medsdata.domain.Views.CalendarDayView;
import com.xhr.medsdata.domain.Views.DayView;
import com.xhr.medsdata.domain.Views.PersonDayView;
import com.xhr.medsdata.domain.Views.PeriodView;
import com.xhr.medsdata.domain.Views.PlanItemView;
import com.xhr.medsdata.repository.PersonRepository;
import com.xhr.medsdata.repository.PlanItemRepository;
import com.xhr.medsdata.repository.PlanRepository;
import com.xhr.medsdata.repository.TakeRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HomeService {

    private final PersonRepository personRepository;
    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final TakeRecordRepository takeRecordRepository;

    public HomeService(PersonRepository personRepository, PlanRepository planRepository,
                       PlanItemRepository planItemRepository, TakeRecordRepository takeRecordRepository) {
        this.personRepository = personRepository;
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.takeRecordRepository = takeRecordRepository;
    }

    public DayView dayView(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        List<Person> persons = personRepository.findAll("ENABLED");
        Map<String, TakeRecord> activeMap = activeRecords(takeRecordRepository.findByDate(target));

        List<PersonDayView> personViews = new ArrayList<>();
        for (Person person : persons) {
            Plan plan = planRepository.findEffective(person.id(), target).orElse(null);
            List<PlanItemView> items = plan == null ? List.of() : planItemRepository.findViews(plan.id());
            Map<String, List<PlanItemView>> byPeriod = new HashMap<>();
            for (PlanItemView item : items) {
                byPeriod.computeIfAbsent(item.period(), k -> new ArrayList<>()).add(item);
            }
            List<PeriodView> periods = new ArrayList<>();
            for (Period period : Period.ordered()) {
                TakeRecord record = activeMap.get(person.id() + ":" + period.name());
                periods.add(new PeriodView(
                        period.name(),
                        period.label(),
                        byPeriod.getOrDefault(period.name(), List.of()),
                        record != null,
                        record == null ? null : record.id(),
                        record == null ? null : record.takenAt(),
                        record == null ? null : record.remark()));
            }
            personViews.add(new PersonDayView(
                    person.id(),
                    person.name(),
                    plan == null ? null : plan.id(),
                    plan == null ? null : plan.versionNo(),
                    plan == null ? "NONE" : deriveStatus(plan, target),
                    plan == null ? null : plan.effectiveFrom(),
                    periods));
        }
        return new DayView(target, personViews);
    }

    public List<CalendarDayView> calendar(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        List<Person> persons = personRepository.findAll("ENABLED");
        Map<String, Plan> effectiveCache = new HashMap<>();
        Map<Long, List<PlanItemView>> itemsCache = new HashMap<>();

        List<CalendarDayView> result = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            final LocalDate current = day;
            int expected = 0;
            int taken = 0;
            for (Person person : persons) {
                String key = person.id() + "@" + current;
                Plan plan = effectiveCache.computeIfAbsent(key,
                        k -> planRepository.findEffective(person.id(), current).orElse(null));
                if (plan == null) {
                    continue;
                }
                List<PlanItemView> items = itemsCache.computeIfAbsent(plan.id(),
                        k -> planItemRepository.findViews(plan.id()));
                Set<String> periods = new HashSet<>();
                for (PlanItemView item : items) {
                    periods.add(item.period());
                }
                if (periods.isEmpty()) {
                    continue;
                }
                expected += periods.size();
                for (TakeRecord record : activeRecords(takeRecordRepository.findByPersonAndDate(person.id(), day)).values()) {
                    if (periods.contains(record.period())) {
                        taken++;
                    }
                }
            }
            result.add(new CalendarDayView(day, expected, taken, expected > 0 && taken >= expected));
        }
        return result;
    }

    private Map<String, TakeRecord> activeRecords(List<TakeRecord> records) {
        Map<String, TakeRecord> map = new HashMap<>();
        for (TakeRecord record : records) {
            if (!"TAKEN".equals(record.status())) {
                continue;
            }
            String key = record.personId() + ":" + record.period();
            TakeRecord current = map.get(key);
            if (current == null || record.id() > current.id()) {
                map.put(key, record);
            }
        }
        return map;
    }

    private String deriveStatus(Plan plan, LocalDate date) {
        if (plan.effectiveFrom().isAfter(date)) {
            return "PENDING";
        }
        if (plan.effectiveTo() == null || !plan.effectiveTo().isBefore(date)) {
            return "CURRENT";
        }
        return "HISTORY";
    }
}
