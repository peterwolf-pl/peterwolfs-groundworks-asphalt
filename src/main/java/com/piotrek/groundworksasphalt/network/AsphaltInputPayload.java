package com.piotrek.groundworksasphalt.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client operator input packet for Asphalt machinery.
 *
 * <p>Transmits:
 * <ul>
 *   <li>throttle (-1.0 to 1.0 for W/S)</li>
 *   <li>steer (-1.0 to 1.0 for A/D)</li>
 *   <li>toolLift (blade lift / screed lift / roller speed)</li>
 *   <li>toolAux (blade angle / hopper fold)</li>
 *   <li>action1 (grading / paving / vibrating toggle)</li>
 *   <li>action2 (ripper / hopper fold / water spray toggle)</li>
 * </ul>
 */
public record AsphaltInputPayload(
        float throttle,
        float steer,
        float toolLift,
        float toolAux,
        boolean action1,
        boolean action2
) implements CustomPacketPayload {

    public static final Type<AsphaltInputPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("pw_groundworks_asphalt", "asphalt_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AsphaltInputPayload> CODEC = new StreamCodec<>() {
        @Override
        public AsphaltInputPayload decode(RegistryFriendlyByteBuf buf) {
            return new AsphaltInputPayload(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, AsphaltInputPayload payload) {
            buf.writeFloat(payload.throttle);
            buf.writeFloat(payload.steer);
            buf.writeFloat(payload.toolLift);
            buf.writeFloat(payload.toolAux);
            buf.writeBoolean(payload.action1);
            buf.writeBoolean(payload.action2);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
