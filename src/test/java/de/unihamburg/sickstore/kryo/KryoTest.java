package de.unihamburg.sickstore.kryo;

import com.esotericsoftware.kryo.Kryo;
import de.unihamburg.sickstore.database.messages.ClientRequestCleanup;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

public class KryoTest {

    @Test
    public void testEncoderDecode() throws CloneNotSupportedException {
        Kryo kryo = new Kryo();
        kryo.register(ClientRequestCleanup.class);

        KryoDecoder kd = new KryoDecoder(kryo);
        KryoEncoder ke = new KryoEncoder(kryo);

        EmbeddedChannel ch = new EmbeddedChannel(ke,kd);
        ch.writeOutbound(new ClientRequestCleanup("export/folder"));
        ch.writeInbound(ch.readOutbound());

        ClientRequestCleanup cleanup = (ClientRequestCleanup) ch.readInbound();
        assertNotNull(cleanup);
        assertEquals( cleanup.getExportFolder(), "export/folder");
    }

}
