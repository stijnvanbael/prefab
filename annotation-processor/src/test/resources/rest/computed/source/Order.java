package rest.computed;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        List<Line> lines) {

    @Create
    public Order(@NotNull List<Line> lines) {
        this(Reference.create(), 0L, new ArrayList<>(lines));
    }

    @Update(path = "/lines", method = "POST")
    public void addLine(@NotNull Line line) {
        lines.add(line);
    }

    @Computed
    public BigDecimal total() {
        return lines.stream().map(Line::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Computed
    public int lineCount() {
        return lines.size();
    }

    public record Line(String product, BigDecimal price) {
        @Computed
        public String display() {
            return product + ": " + price;
        }
    }
}
