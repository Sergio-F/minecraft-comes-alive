package mca.packets;

import io.netty.buffer.ByteBuf;
import mca.core.MCA;
import mca.tile.TileTombstone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import radixcore.packets.AbstractPacket;

public class PacketTombstoneUpdateGet extends AbstractPacket implements IMessage, IMessageHandler<PacketTombstoneUpdateGet, IMessage>
{
	private int x;
	private int y;
	private int z;

	public PacketTombstoneUpdateGet()
	{
	}

	public PacketTombstoneUpdateGet(TileTombstone tombstone)
	{
		this.x = tombstone.xCoord;
		this.y = tombstone.yCoord;
		this.z = tombstone.zCoord;
	}

	@Override
	public void fromBytes(ByteBuf byteBuf)
	{	
		x = byteBuf.readInt();
		y = byteBuf.readInt();
		z = byteBuf.readInt();
	}

	@Override
	public void toBytes(ByteBuf byteBuf)
	{	
		byteBuf.writeInt(x);
		byteBuf.writeInt(y);
		byteBuf.writeInt(z);
	}

	@Override
	public IMessage onMessage(PacketTombstoneUpdateGet packet, MessageContext context)
	{
		final EntityPlayer player = this.getPlayer(context);
		final World world = player.worldObj;
		
		try
		{
			final TileTombstone tombstone = (TileTombstone)world.getTileEntity(packet.x, packet.y, packet.z);
			MCA.getPacketHandler().sendPacketToPlayer(new PacketTombstoneUpdateSet(tombstone), (EntityPlayerMP) player);
		}
		
		catch (ClassCastException e)
		{
			//Throw away these exceptions.
		}
		
		return null;
	}
}
