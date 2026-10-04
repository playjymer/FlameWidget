package com.flamewidget.app

import android.app.Application
import com.flamewidget.app.widget.WidgetRefreshWorker

class FlameWidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetRefreshWorker.schedulePeriodicUpdates(this)
    }
}
