package org.metaborg.spoofax.eclipse.util;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class BundleUtils {
    public static Map<String, Bundle> bundlesBySymbolicName(BundleContext context) {
        final Map<String, Bundle> bundles = new HashMap<>();
        for(Bundle bundle : context.getBundles()) {
            bundles.put(bundle.getSymbolicName(), bundle);
        }
        return bundles;
    }
}
