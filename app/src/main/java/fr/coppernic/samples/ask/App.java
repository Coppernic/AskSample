package fr.coppernic.samples.ask;

import android.app.Application;
import android.view.Window;
import android.view.WindowManager;

import timber.log.Timber;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
