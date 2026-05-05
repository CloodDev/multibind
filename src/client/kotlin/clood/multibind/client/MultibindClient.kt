package clood.multibind.client

import clood.multibind.MultibindBindings
import net.fabricmc.api.ClientModInitializer

object MultibindClient : ClientModInitializer {
	override fun onInitializeClient() {
		MultibindBindings.load()
	}
}