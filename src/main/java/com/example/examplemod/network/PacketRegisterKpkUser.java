package com.example.examplemod.network;

import com.example.examplemod.Gender;
import com.example.examplemod.User;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.kpk.KPKServerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PacketRegisterKpkUser implements IMessage {
	private String familiya;
	private String name;
	private String pozivnoy;
	private String gender;
	private String birthdate;

	public PacketRegisterKpkUser() {}

	public PacketRegisterKpkUser(String familiya, String name, String pozivnoy, String gender, String birthdate) {
		this.familiya = familiya;
		this.name = name;
		this.pozivnoy = pozivnoy;
		this.gender = gender;
		this.birthdate = birthdate;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.familiya = ByteBufUtils.readUTF8String(buf);
		this.name = ByteBufUtils.readUTF8String(buf);
		this.pozivnoy = ByteBufUtils.readUTF8String(buf);
		this.gender = ByteBufUtils.readUTF8String(buf);
		this.birthdate = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, this.familiya);
		ByteBufUtils.writeUTF8String(buf, this.name);
		ByteBufUtils.writeUTF8String(buf, this.pozivnoy);
		ByteBufUtils.writeUTF8String(buf, this.gender);
		ByteBufUtils.writeUTF8String(buf, this.birthdate);
	}

	public static class Handler implements IMessageHandler<PacketRegisterKpkUser, IMessage> {
		private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		@Override
		public IMessage onMessage(PacketRegisterKpkUser message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServer().addScheduledTask(() -> {
				// Validate item in hand
				ItemStack kpk = player.getHeldItemMainhand();
				if (!(kpk.getItem() instanceof ItemKPK)) {
					kpk = player.getHeldItemOffhand();
				}
				if (!(kpk.getItem() instanceof ItemKPK)) {
					player.sendMessage(new TextComponentString(TextFormatting.RED + "Нужно держать КПК в руке для регистрации."));
					return;
				}

				// Basic validation
				String fam = safe(message.familiya);
				String nam = safe(message.name);
				String poz = safe(message.pozivnoy);
				String gen = safe(message.gender);
				String bdt = safe(message.birthdate);

				if (fam.isEmpty() || nam.isEmpty() || poz.isEmpty() || gen.isEmpty() || bdt.isEmpty()) {
					player.sendMessage(new TextComponentString(TextFormatting.RED + "Все поля обязательны."));
					return;
				}
				if (poz.length() < 2 || poz.length() > 20) {
					player.sendMessage(new TextComponentString(TextFormatting.RED + "Позывной: 2-20 символов."));
					return;
				}
			Gender gender;
				try {
				Gender parsed = Gender.fromString(gen);
				gender = parsed != null ? parsed : Gender.valueOf(gen);
				} catch (Exception e) {
				player.sendMessage(new TextComponentString(TextFormatting.RED + "Пол: допустимо М/Ж или Мужской/Женский."));
					return;
				}
				LocalDate birthdate;
				try {
					birthdate = LocalDate.parse(bdt, DATE_FORMATTER);
				} catch (Exception e) {
					player.sendMessage(new TextComponentString(TextFormatting.RED + "Дата рождения в формате dd.MM.yyyy."));
					return;
				}

				User user = new User(fam, nam, poz, gender, birthdate);
				// Save to item NBT
				ItemKPK.setUserData(kpk, user);
				// Save to world database
				KPKServerManager.setUser(player.getUniqueID(), user);
				player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Регистрация КПК выполнена."));
			});
			return null;
		}

		private String safe(String s) {
			return s == null ? "" : s.trim();
		}
	}
}
