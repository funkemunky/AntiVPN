package dev.brighten.antivpn.database.sqllite.version.impl;

import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import dev.brighten.antivpn.database.sqllite.version.Version;

public class First implements Version {

    @Override
    public void update(LiteDatabase database) {

    }

    @Override
    public int versionNumber() {
        return 1;
    }
}
