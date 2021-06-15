package dev.brighten.antivpn;

public class AntiVPN {

    private static AntiVPN INSTANCE;

    public static void start() {
        INSTANCE = new AntiVPN();
    }

    public static AntiVPN getInstance() {
        assert INSTANCE != null: "AntiVPN has not been initialized!";

        return INSTANCE;
    }

    public void getAPI()
}
