package emanondev.deepdungeons.populator;

/**
 * Represents a paper populator's priority in execution.
 * <p>
 * Populators with lower priority are called first will listeners with higher priority are called last.
 * <p>
 * Listeners are called in following order: LOWEST -> LOW -> NORMAL -> HIGH -> HIGHEST -> MONITOR
 */
public enum PopulatorPriority {
    LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
}
