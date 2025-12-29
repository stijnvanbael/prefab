package pubsub.multitopic;

import be.appify.prefab.core.annotations.RepositoryMixin;

import java.time.LocalDate;
import java.util.List;

@RepositoryMixin(DayTotal.class)
public interface DayTotalRepositoryMixin {
    public List<DayTotal> findByDate(LocalDate date);
}