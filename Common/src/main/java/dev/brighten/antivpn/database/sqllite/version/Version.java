package dev.brighten.antivpn.database.sqllite.version;

import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import dev.brighten.antivpn.database.sqllite.version.impl.First;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface Version {
    void update(LiteDatabase database) throws SQLException;
    int versionNumber();

    List<Version> versions = new ArrayList<>();

    static void register() {
        versions.add(new First());
    }
}
