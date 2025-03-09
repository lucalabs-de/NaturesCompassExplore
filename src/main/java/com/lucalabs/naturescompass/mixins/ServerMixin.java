package com.lucalabs.naturescompass.mixins;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lucalabs.naturescompass.workers.WorldWorkerManager;

import net.minecraft.server.MinecraftServer;

// In theory this could be achieved with Fabric events, but I worry that the original author knows something I don't, so
// I won't touch this.
@Mixin(MinecraftServer.class)
public class ServerMixin {

	@Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "HEAD"))
	private void startTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		WorldWorkerManager.tick(true);
	}
	
	@Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "TAIL"))
	private void endTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		WorldWorkerManager.tick(false);
	}
	
	@Inject(method = "shutdown()V", at = @At(value = "TAIL"))
	private void onShutdown(CallbackInfo ci) {
		WorldWorkerManager.clear();
	}
	
}
