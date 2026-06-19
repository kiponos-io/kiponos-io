package io.kiponos;

import io.kiponos.sdk.Kiponos;

import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    static Kiponos kiponos = Kiponos.createForCurrentTeam();

    public static void main(String[] args) throws InterruptedException {
        String springBootStarterURL = kiponos.path("useful-urls", "Development", "Java", "Frameworks-and-Libraries", "SpringBoot").get("starter");
        System.out.println("springBootStarterURL: " + springBootStarterURL);
        TimeUnit.SECONDS.sleep(5);
        kiponos.disconnect();
    }
}