package dev.brighten.antivpn.database.sql.utils;

import java.sql.ResultSet;

public interface ResultSetIterator {
    void next(ResultSet rs) throws Exception;
}
