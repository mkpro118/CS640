public interface ITCPPacket {
    public byte[] serialize();
    public ITCPPacket deserialize(byte[] packet);
    public short checksum();
}
