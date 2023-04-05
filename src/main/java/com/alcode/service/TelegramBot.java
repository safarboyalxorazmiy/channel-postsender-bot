package com.alcode.service;

import com.alcode.config.BotConfig;
import com.alcode.user.Role;
import com.alcode.user.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${channel.id}")
    private Long channelId;

    private final BotConfig config;
    private final UsersService usersService;
    private final AttachService attachService;

    public TelegramBot(BotConfig config, UsersService usersService, AttachService attachService) {
        this.config = config;
        this.usersService = usersService;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Boshlash"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error during setting bot's command list: {}", e.getMessage());
        }

        this.attachService = attachService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            if (update.getMessage().getChat().getType().equals("supergroup")) {
                // DO NOTHING CHANNEL CHAT ID IS -1001764816733
                return;
            } else {
                Role role = usersService.getRoleByChatId(chatId);

                if (update.hasMessage() && update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();

                    if (messageText.startsWith("/")) {
                        if (messageText.startsWith("/login ")) {
                            String password = messageText.substring(7);

                            if (password.equals("Xp2s5v8y/B?E(H+KbPeShVmYq3t6w9z$C&F)J@NcQfTjWnZr4u7x!A%D*G-KaPdSgUkXp2s5v8y/B?E(H+MbQeThWmYq3t6w9z$C&F)J@NcRfUjXn2r4u7x!A%D*G-Ka")) {
                                usersService.changeRole(chatId, Role.ROLE_ADMIN);
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                                return;
                            }
                            return;
                        }

                        switch (messageText) {
                            case "/start" -> {
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                                return;
                            }
                            case "/help" -> {
                                helpCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                                return;
                            }
                            default -> {
                                sendMessage(chatId, "Sorry, command was not recognized");
                                return;
                            }
                        }
                    }

                    if (role.equals(Role.ROLE_ADMIN)) {

                    }
                    else if (role.equals(Role.ROLE_USER)) {}
                }
                if (update.hasMessage() && update.getMessage().hasPhoto()) {
                    if (role != Role.ROLE_ADMIN) {
                        deleteMessageById(chatId, update.getMessage().getMessageId());
                        return;
                    }

                    List<PhotoSize> photo = update.getMessage().getPhoto();

                    try {
                        GetFile getFile = new GetFile(photo.get(photo.size()-1).getFileId());
                        org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
                        String fileUrl = tgFile.getFileUrl(getBotToken());
                        String localUrl = attachService.saveImageFromUrl(fileUrl);



                        sendMessageToChannel(localUrl, update.getMessage().getCaption());
                        sendMessage(chatId, "Maqola yaratildi ✅");
                    } catch (TelegramApiException ignored) {

                    }

                }
            }

        }
    }

    private void startCommandReceived(long chatId, String firstName, String lastName) {
        Role role = usersService.createUser(chatId, firstName, lastName).getRole();

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.enableHtml(true);

        if (role.equals(Role.ROLE_USER)) {
            message.setText("Welcome User, What's up?");
        } else if (role.equals(Role.ROLE_ADMIN)) {
            message.setText("Welcome Admin, What's up?");
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in startCommandReceived()");
        }
    }

    private void helpCommandReceived(long chatId, String firstName) {
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableHtml(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    public void deleteMessageById(Long chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);

            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToChannel(String photoUrl, String content) {
        try {
            File file = new File(photoUrl);
            String extension = getExtension(photoUrl);

            if (extension.equalsIgnoreCase(".mp4")) {
                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(channelId);
                sendVideo.setCaption(content);

                InputFile inputFile = new InputFile();
                inputFile.setMedia(file, file.getName());
                sendVideo.setVideo(inputFile);
                sendVideo.setParseMode("HTML");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("\uD83D\uDECD Buyurtma berish");
                button.setUrl("https://t.me/BazaarManager");
                rowInLine.add(button);

                rows.add(rowInLine);
                inlineKeyboardMarkup.setKeyboard(rows);
                sendVideo.setReplyMarkup(inlineKeyboardMarkup);
                execute(sendVideo);
                return;
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(channelId);
            sendPhoto.setCaption(content);

            InputFile inputFile = new InputFile();
            inputFile.setMedia(file, file.getName());
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setParseMode("HTML");

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("\uD83D\uDECD Buyurtma berish");
            button.setUrl("https://t.me/BazaarManager");
            rowInLine.add(button);

            rows.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rows);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
            execute(sendPhoto);
        } catch (RuntimeException | TelegramApiException e) {
            log.warn("There is a problems during sending a photos, {}", e);
        }
    }

    public String getExtension(String fileName) {
        // mp3/jpg/npg/mp4.....
        if (fileName == null) {
            throw new RuntimeException("File name null");
        }
        int lastIndex = fileName.lastIndexOf(".");
        return fileName.substring(lastIndex + 1);
    }
}