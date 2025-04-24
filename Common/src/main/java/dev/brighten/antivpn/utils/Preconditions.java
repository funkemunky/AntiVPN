//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.brighten.antivpn.utils;

public final class Preconditions {
    private Preconditions() {
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        } else {
            return reference;
        }
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        } else {
            return reference;
        }
    }

    public static <T> T checkNotNull(T reference, String errorMessageTemplate, Object... errorMessageArgs) {
        if (reference == null) {
            throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
        } else {
            return reference;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, char p1) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, int p1) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, long p1) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, char p1, char p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, char p1, int p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, char p1, long p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, char p1, Object p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, int p1, char p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, int p1, int p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, int p1, long p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, int p1, Object p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, long p1, char p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, long p1, int p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, long p1, long p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, long p1, Object p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, char p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, int p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, long p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, Object p2) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, Object p2, Object p3) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2, p3));
        } else {
            return obj;
        }
    }

    public static <T> T checkNotNull(T obj, String errorMessageTemplate, Object p1, Object p2, Object p3, Object p4) {
        if (obj == null) {
            throw new NullPointerException(format(errorMessageTemplate, p1, p2, p3, p4));
        } else {
            return obj;
        }
    }

    static String format(String template, Object... args) {
        template = String.valueOf(template);
        StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;

        int i;
        int placeholderStart;
        for(i = 0; i < args.length; templateStart = placeholderStart + 2) {
            placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }

            builder.append(template, templateStart, placeholderStart);
            builder.append(args[i++]);
        }

        builder.append(template, templateStart, template.length());
        if (i < args.length) {
            builder.append(" [");
            builder.append(args[i++]);

            while(i < args.length) {
                builder.append(", ");
                builder.append(args[i++]);
            }

            builder.append(']');
        }

        return builder.toString();
    }
}
