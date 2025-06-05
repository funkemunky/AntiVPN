package dev.brighten.antivpn.database.sqllite.version.impl;

import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sqllite.version.Version;

public class First implements Version {

    @Override
    public void update(VPNDatabase database) {

    }

    @Override
    public int versionNumber() {
        return 1;
    }
}
