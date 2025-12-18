package wile.anthilloutside.libmc;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.function.BiConsumer;

public class Networking {
    public record OverlayTextMessagePayload(Component message, int delay) implements CustomPacketPayload {
        // Replace "mymod" with your actual mod ID
        public static final Type<OverlayTextMessagePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath("anthilloutside", "otms2c"));

        // Codec for automatic serialization/deserialization
        public static final StreamCodec<RegistryFriendlyByteBuf, OverlayTextMessagePayload> CODEC = StreamCodec
                .composite(
                        ComponentSerialization.TRUSTED_STREAM_CODEC, OverlayTextMessagePayload::message,
                        ByteBufCodecs.VAR_INT, OverlayTextMessagePayload::delay,
                        OverlayTextMessagePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public class OverlayTextMessage {
        protected static BiConsumer<Component, Integer> handler_ = null;
        public static final int DISPLAY_TIME_MS = 3000;

        public static void setHandler(BiConsumer<Component, Integer> handler) {
            if (handler_ == null)
                handler_ = handler;
        }

        public static void sendToPlayer(ServerPlayer player, Component message) {
            sendToPlayer(player, message, DISPLAY_TIME_MS);
        }

        public static void sendToPlayer(ServerPlayer player, Component message, int delay) {
            // Validation (Assuming Auxiliaries.isEmpty is your own helper)
            if (message == null || message.getString().isEmpty())
                return;

            // Send using NeoForge's PacketDistributor
            PacketDistributor.sendToPlayer(player, new OverlayTextMessagePayload(message, delay));
        }

        // Client-side handling logic
        public static void handleClient(final OverlayTextMessagePayload payload, final IPayloadContext context) {
            context.enqueueWork(() -> {
                if (handler_ != null) {
                    handler_.accept(payload.message(), payload.delay());
                }
            });
        }
    }

    @EventBusSubscriber(modid = "anthilloutside", bus = EventBusSubscriber.Bus.MOD)
    public class ModNetworking {
        @SubscribeEvent
        public static void register(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1"); // Network version

            registrar.playToClient(
                    OverlayTextMessagePayload.TYPE,
                    OverlayTextMessagePayload.CODEC,
                    OverlayTextMessage::handleClient);
        }
    }
}