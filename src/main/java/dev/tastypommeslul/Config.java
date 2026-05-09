package dev.tastypommeslul;

import com.moulberry.lattice.LatticeDynamicFrequency;
import com.moulberry.lattice.annotation.LatticeOption;
import com.moulberry.lattice.annotation.constraint.LatticeEnableIf;
import com.moulberry.lattice.annotation.constraint.LatticeIntRange;
import com.moulberry.lattice.annotation.widget.LatticeWidgetButton;
import com.moulberry.lattice.annotation.widget.LatticeWidgetSlider;

public class Config {
    @LatticeOption(title = "lucyeatreminder.debug", description = "!!.desc")
    @LatticeWidgetButton
    public boolean debug = false;

    @LatticeOption(title = "lucyeatreminder.enabled", description = "!!.desc")
    @LatticeWidgetButton
    public boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    @LatticeOption(title = "lucyeatreminder.amount", description = "!!.desc")
    @LatticeEnableIf(function = "isEnabled", frequency = LatticeDynamicFrequency.EVERY_TICK)
    @LatticeWidgetSlider
    @LatticeIntRange(min = 0, max = 10)
    public int amount = 5;
}
