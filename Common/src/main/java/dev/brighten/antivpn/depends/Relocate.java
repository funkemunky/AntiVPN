package dev.brighten.antivpn.depends;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Relocate {
    String from();
    String to();
}