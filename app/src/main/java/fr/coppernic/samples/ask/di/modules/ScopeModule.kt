package fr.coppernic.samples.ask.di.modules

import fr.coppernic.samples.ask.home.HomeFragment
import fr.coppernic.samples.ask.home.HomePresenter
import org.koin.core.qualifier.named
import org.koin.dsl.module


val scopesModule by lazy {
    module {
        scope(named<HomeFragment>()) {
            scoped { HomePresenter() }
        }
    }
}
