package metrics;

import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;

//@formatter:off
@PluginInfo(
        status = PluginStatus.UNSTABLE,
        packageName = GhidraMetricsToolkitPlugin.PACKAGE_NAME,
        category = PluginCategoryNames.EXAMPLES,
        shortDescription = "Collection of Metrics",
        description = "This plugin provides a collection of metrics to be computed on a native binary"
)
//@formatter:on
public class GhidraMetricsToolkitPlugin extends ProgramPlugin {

    public static final String PACKAGE_NAME = "metrics";
    GhidraMetricsToolkitProvider provider;

    public GhidraMetricsToolkitPlugin(PluginTool plugintool) {
        super(plugintool);
        String pluginName = getName();
        provider = new GhidraMetricsToolkitProvider(this, pluginName);
    }

    @Override
    protected void programActivated(Program program) {
        provider.handleProgramActivated();
        super.programActivated(program);
    }

    @Override
    protected void locationChanged(ProgramLocation loc) {
        provider.handleLocationChanged();
        super.locationChanged(loc);
    }
}
