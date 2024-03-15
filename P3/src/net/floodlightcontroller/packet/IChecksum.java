package net.floodlightcontroller.packet;

// import net.floodlightcontroller.packet.IPacket;

public interface IChecksum<T> extends IPacket {
    public short getChecksum();

    public T setChecksum(short checksum);
}
