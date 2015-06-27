/*
   JRakLib networking library.
   This software is not affiliated with RakNet or Jenkins Software LLC.
   This software is a port of PocketMine/RakLib <https://github.com/PocketMine/RakLib>.
   All credit goes to the PocketMine Project (http://pocketmine.net)
 
   Copyright (C) 2015  JRakLib Project

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.beaconpe.jraklib.protocol;

/**
 * Big class with lots of DataPackets
 */
public abstract class DataPackets {
    public static class DATA_PACKET_0 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x80;
        }
    }
    public static class DATA_PACKET_1 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x81;
        }
    }
    public static class DATA_PACKET_2 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x82;
        }
    }
    public static class DATA_PACKET_3 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x83;
        }
    }
    public static class DATA_PACKET_4 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x84;
        }
    }
    public static class DATA_PACKET_5 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x85;
        }
    }
    public static class DATA_PACKET_6 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x86;
        }
    }
    public static class DATA_PACKET_7 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x87;
        }
    }
    public static class DATA_PACKET_8 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x88;
        }
    }
    public static class DATA_PACKET_9 extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x89;
        }
    }
    public static class DATA_PACKET_A extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8a;
        }
    }
    public static class DATA_PACKET_B extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8b;
        }
    }
    public static class DATA_PACKET_C extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8c;
        }
    }
    public static class DATA_PACKET_D extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8d;
        }
    }
    public static class DATA_PACKET_E extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8e;
        }
    }
    public static class DATA_PACKET_F extends DataPacket{
        @Override
        public byte getID() {
            return (byte) 0x8f;
        }
    }
}
