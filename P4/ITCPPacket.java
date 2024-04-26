public interface ITCPPacket {
    public short checksum();
    public int getSequenceNumber();
    public int getAcknowledgement();
    public long getTimeStamp();

    public boolean isSyn();
    public boolean isAck();
    public boolean isFin();
    public void setFlag(TCPFlag flag, boolean on);

    public byte[] getPayload();
    public void setPayload(byte[] payload);

    public byte[] serialize();
    public ITCPPacket deserialize(byte[] packet);
}
