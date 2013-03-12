package org.solovyev.android.messenger.chats;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import javax.annotation.Nonnull;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.solovyev.android.db.AbstractDbQuery;
import org.solovyev.android.db.AbstractObjectDbExec;
import org.solovyev.android.db.AbstractSQLiteHelper;
import org.solovyev.android.db.AndroidDbUtils;
import org.solovyev.android.db.DbExec;
import org.solovyev.android.db.DeleteAllRowsDbExec;
import org.solovyev.android.db.ListMapper;
import org.solovyev.android.db.properties.PropertyByIdDbQuery;
import org.solovyev.android.messenger.MergeDaoResult;
import org.solovyev.android.messenger.MergeDaoResultImpl;
import org.solovyev.android.messenger.db.StringIdMapper;
import org.solovyev.android.messenger.messages.SqliteChatMessageDao;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.messenger.users.UserService;
import org.solovyev.android.properties.AProperty;
import org.solovyev.common.collections.Collections;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * User: serso
 * Date: 6/6/12
 * Time: 3:27 PM
 */
@Singleton
public class SqliteChatDao extends AbstractSQLiteHelper implements ChatDao {

    /*
    **********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */

    @Inject
    @Nonnull
    private UserService userService;

    @Inject
    public SqliteChatDao(@Nonnull Application context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
        super(context, sqliteOpenHelper);
    }

    SqliteChatDao(@Nonnull Context context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
        super(context, sqliteOpenHelper);
    }

    @Nonnull
    @Override
    public List<String> loadUserChatIds(@Nonnull String userId) {
        return AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadChatIdsByUserId(getContext(), userId, getSqliteOpenHelper()));
    }

    @Override
    public void updateChat(@Nonnull Chat chat) {
        AndroidDbUtils.doDbExecs(getSqliteOpenHelper(), Arrays.<DbExec>asList(new UpdateChat(chat), new DeleteChatProperties(chat), new InsertChatProperties(chat)));
    }

    @Override
    public void deleteAllChats() {
        AndroidDbUtils.doDbExec(getSqliteOpenHelper(), DeleteAllRowsDbExec.newInstance("user_chats"));
        AndroidDbUtils.doDbExec(getSqliteOpenHelper(), DeleteAllRowsDbExec.newInstance("chat_properties"));
        AndroidDbUtils.doDbExec(getSqliteOpenHelper(), DeleteAllRowsDbExec.newInstance("chats"));
    }

    @Nonnull
    @Override
    public List<String> loadChatIds() {
        return AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadChatIds(getContext(), getSqliteOpenHelper()));
    }

    @Nonnull
    @Override
    public List<AProperty> loadChatPropertiesById(@Nonnull String chatId) {
        return AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadChatPropertiesDbQuery(chatId, getContext(), getSqliteOpenHelper()));
    }

    @Nonnull
    @Override
    public List<Chat> loadUserChats(@Nonnull String userId) {
        return AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadChatsByUserId(getContext(), userId, getSqliteOpenHelper(), this));
    }

    @Nonnull
    @Override
    public List<User> loadChatParticipants(@Nonnull String chatId) {
        return AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadChatParticipants(getContext(), chatId, userService, getSqliteOpenHelper()));
    }

    @Override
    public Chat loadChatById(@Nonnull String chatId) {
        return Collections.getFirstListElement(AndroidDbUtils.doDbQuery(getSqliteOpenHelper(), new LoadByChatId(getContext(), chatId, getSqliteOpenHelper(), this)));
    }

    private static final class LoadChatParticipants extends AbstractDbQuery<List<User>> {

        @Nonnull
        private final String chatId;

        @Nonnull
        private final UserService userService;

        private LoadChatParticipants(@Nonnull Context context,
                                     @Nonnull String chatId,
                                     @Nonnull UserService userService,
                                     @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
            super(context, sqliteOpenHelper);
            this.chatId = chatId;
            this.userService = userService;
        }

        @Nonnull
        @Override
        public Cursor createCursor(@Nonnull SQLiteDatabase db) {
            return db.query("user_chats", null, "chat_id = ? ", new String[]{chatId}, null, null, null);
        }

        @Nonnull
        @Override
        public List<User> retrieveData(@Nonnull Cursor cursor) {
            return new ListMapper<User>(new ChatParticipantMapper(userService)).convert(cursor);
        }
    }

    private static final class LoadChatsByUserId extends AbstractDbQuery<List<Chat>> {

        @Nonnull
        private final String userId;

        @Nonnull
        private final ChatDao chatDao;

        private LoadChatsByUserId(@Nonnull Context context, @Nonnull String userId, @Nonnull SQLiteOpenHelper sqliteOpenHelper, @Nonnull ChatDao chatDao) {
            super(context, sqliteOpenHelper);
            this.userId = userId;
            this.chatDao = chatDao;
        }

        @Nonnull
        @Override
        public Cursor createCursor(@Nonnull SQLiteDatabase db) {
            return db.query("chats", null, "id in (select chat_id from user_chats where user_id = ? ) ", new String[]{userId}, null, null, null);
        }

        @Nonnull
        @Override
        public List<Chat> retrieveData(@Nonnull Cursor cursor) {
            return new ListMapper<Chat>(new ChatMapper(chatDao)).convert(cursor);
        }
    }

    public static final class LoadChatPropertiesDbQuery extends PropertyByIdDbQuery {

        public LoadChatPropertiesDbQuery(@Nonnull String chatId, @Nonnull Context context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
            super(context, sqliteOpenHelper, "chat_properties", "chat_id", chatId);
        }
    }

    @Nonnull
    @Override
    public MergeDaoResult<ApiChat, String> mergeUserChats(@Nonnull String userId, @Nonnull List<? extends ApiChat> apiChats) {
        final MergeDaoResultImpl<ApiChat, String> result = new MergeDaoResultImpl<ApiChat, String>(apiChats);

        final List<String> chatsFromDb = loadUserChatIds(userId);
        for (final String chatIdFromDb : chatsFromDb) {
            try {
                // chat exists both in db and on remote server => just update chat properties
                result.addUpdatedObject(Iterables.find(apiChats, new ChatByIdFinder(chatIdFromDb)));
            } catch (NoSuchElementException e) {
                // !!! actually not all chats are loaded and we cannot delete the chat just because it is not in the list

                // chat was removed on remote server => need to remove from local db
                //result.addRemovedObjectId(chatIdFromDb);
            }
        }

        final List<String> chatIdsFromDb = loadChatIds();
        for (ApiChat apiChat : apiChats) {
            try {
                // chat exists both in db and on remote server => case already covered above
                Iterables.find(chatsFromDb, Predicates.equalTo(apiChat.getChat().getEntity().getEntityId()));
            } catch (NoSuchElementException e) {
                // chat was added on remote server => need to add to local db
                if (chatIdsFromDb.contains(apiChat.getChat().getEntity().getEntityId())) {
                    // only link must be added - chat already in chats table
                    result.addAddedObjectLink(apiChat);
                } else {
                    // no chat information in local db is available - full chat insertion
                    result.addAddedObject(apiChat);
                }
            }
        }

        final List<DbExec> execs = new ArrayList<DbExec>();

        if (!result.getRemovedObjectIds().isEmpty()) {
            execs.addAll(RemoveChats.newInstances(userId, result.getRemovedObjectIds()));
        }

        for (ApiChat updatedChat : result.getUpdatedObjects()) {
            execs.add(new UpdateChat(updatedChat.getChat()));
            execs.add(new DeleteChatProperties(updatedChat.getChat()));
            execs.add(new InsertChatProperties(updatedChat.getChat()));
        }

        for (ApiChat addedChatLink : result.getAddedObjectLinks()) {
            execs.add(new UpdateChat(addedChatLink.getChat()));
            execs.add(new DeleteChatProperties(addedChatLink.getChat()));
            execs.add(new InsertChatProperties(addedChatLink.getChat()));
            execs.add(new InsertChatLink(userId, addedChatLink.getChat().getEntity().getEntityId()));
        }

        for (ApiChat addedChat : result.getAddedObjects()) {
            execs.add(new InsertChat(addedChat.getChat()));
            execs.add(new InsertChatProperties(addedChat.getChat()));
            execs.add(new InsertChatLink(userId, addedChat.getChat().getEntity().getEntityId()));
            for (ChatMessage chatMessage : addedChat.getMessages()) {
                execs.add(new SqliteChatMessageDao.InsertMessage(addedChat.getChat(), chatMessage));
            }

            for (User participant : addedChat.getParticipants()) {
                if (!participant.getEntity().getEntityId().equals(userId)) {
                    execs.add(new InsertChatLink(participant.getEntity().getEntityId(), addedChat.getChat().getEntity().getEntityId()));
                }
            }
        }

        AndroidDbUtils.doDbExecs(getSqliteOpenHelper(), execs);

        return result;
    }

    private static final class LoadChatIds extends AbstractDbQuery<List<String>> {

        private LoadChatIds(@Nonnull Context context, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
            super(context, sqliteOpenHelper);
        }

        @Nonnull
        @Override
        public Cursor createCursor(@Nonnull SQLiteDatabase db) {
            return db.query("chats", new String[]{"id"}, null, null, null, null, null);
        }

        @Nonnull
        @Override
        public List<String> retrieveData(@Nonnull Cursor cursor) {
            return new ListMapper<String>(StringIdMapper.getInstance()).convert(cursor);
        }
    }


    private static final class LoadChatIdsByUserId extends AbstractDbQuery<List<String>> {

        @Nonnull
        private final String userId;

        private LoadChatIdsByUserId(@Nonnull Context context, @Nonnull String userId, @Nonnull SQLiteOpenHelper sqliteOpenHelper) {
            super(context, sqliteOpenHelper);
            this.userId = userId;
        }

        @Nonnull
        @Override
        public Cursor createCursor(@Nonnull SQLiteDatabase db) {
            return db.query("chats", null, "id in (select chat_id from user_chats where user_id = ? ) ", new String[]{userId}, null, null, null);
        }

        @Nonnull
        @Override
        public List<String> retrieveData(@Nonnull Cursor cursor) {
            return new ListMapper<String>(StringIdMapper.getInstance()).convert(cursor);
        }
    }

    private static class ChatByIdFinder implements Predicate<ApiChat> {

        @Nonnull
        private final String chatId;

        public ChatByIdFinder(@Nonnull String chatId) {
            this.chatId = chatId;
        }

        @Override
        public boolean apply(@javax.annotation.Nullable ApiChat apiChat) {
            return apiChat != null && chatId.equals(apiChat.getChat().getEntity().getEntityId());
        }
    }

    private static final class RemoveChats implements DbExec {

        @Nonnull
        private String userId;

        @Nonnull
        private List<String> chatIds;

        private RemoveChats(@Nonnull String userId, @Nonnull List<String> chatIds) {
            this.userId = userId;
            this.chatIds = chatIds;
        }

        @Nonnull
        private static List<RemoveChats> newInstances(@Nonnull String userId, @Nonnull List<String> chatIds) {
            final List<RemoveChats> result = new ArrayList<RemoveChats>();

            for (List<String> chatIdsChunk : Collections.split(chatIds, MAX_IN_COUNT)) {
                result.add(new RemoveChats(userId, chatIdsChunk));
            }

            return result;
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            db.delete("user_chats", "user_id = ? and chat_id in " + AndroidDbUtils.inClause(chatIds), AndroidDbUtils.inClauseValues(chatIds, userId));
        }
    }


    private static final class UpdateChat extends AbstractObjectDbExec<Chat> {

        private UpdateChat(@Nonnull Chat chat) {
            super(chat);
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            final Chat chat = getNotNullObject();

            final ContentValues values = toContentValues(chat);

            db.update("chats", values, "id = ?", new String[]{String.valueOf(chat.getEntity().getEntityId())});
        }
    }

    private static final class InsertChat extends AbstractObjectDbExec<Chat> {

        private InsertChat(@Nonnull Chat chat) {
            super(chat);
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            final Chat chat = getNotNullObject();

            final ContentValues values = toContentValues(chat);

            db.insert("chats", null, values);
        }
    }

    @Nonnull
    private static ContentValues toContentValues(@Nonnull Chat chat) {
        final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.basicDateTime();

        final DateTime lastMessagesSyncDate = chat.getLastMessagesSyncDate();

        final ContentValues values = new ContentValues();

        values.put("id", chat.getEntity().getEntityId());
        values.put("realm_id", chat.getEntity().getRealmId());
        values.put("realm_chat_id", chat.getEntity().getRealmEntityId());
        values.put("messages_count", chat.getMessagesCount());
        values.put("last_messages_sync_date", lastMessagesSyncDate == null ? null : dateTimeFormatter.print(lastMessagesSyncDate));

        return values;
    }

    private static final class DeleteChatProperties extends AbstractObjectDbExec<Chat> {

        private DeleteChatProperties(@Nonnull Chat chat) {
            super(chat);
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            final Chat chat = getNotNullObject();

            db.delete("chat_properties", "chat_id = ?", new String[]{String.valueOf(chat.getEntity().getEntityId())});
        }
    }

    private static final class InsertChatProperties extends AbstractObjectDbExec<Chat> {

        private InsertChatProperties(@Nonnull Chat chat) {
            super(chat);
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            final Chat chat = getNotNullObject();

            for (AProperty property : chat.getProperties()) {
                final ContentValues values = new ContentValues();
                values.put("chat_id", chat.getEntity().getEntityId());
                values.put("property_name", property.getName());
                values.put("property_value", property.getValue());
                db.insert("chat_properties", null, values);
            }
        }
    }

    private static final class InsertChatLink implements DbExec {

        @Nonnull
        private String userId;

        @Nonnull
        private String chatId;

        private InsertChatLink(@Nonnull String userId, @Nonnull String chatId) {
            this.userId = userId;
            this.chatId = chatId;
        }

        @Override
        public void exec(@Nonnull SQLiteDatabase db) {
            final ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("chat_id", chatId);
            db.insert("user_chats", null, values);
        }
    }

    private static final class LoadByChatId extends AbstractDbQuery<List<Chat>> {

        @Nonnull
        private final String chatId;

        @Nonnull
        private final ChatDao chatDao;

        private LoadByChatId(@Nonnull Context context, @Nonnull String chatId, @Nonnull SQLiteOpenHelper sqliteOpenHelper, @Nonnull ChatDao chatDao) {
            super(context, sqliteOpenHelper);
            this.chatId = chatId;
            this.chatDao = chatDao;
        }

        @Nonnull
        @Override
        public Cursor createCursor(@Nonnull SQLiteDatabase db) {
            return db.query("chats", null, "id = ? ", new String[]{String.valueOf(chatId)}, null, null, null);
        }

        @Nonnull
        @Override
        public List<Chat> retrieveData(@Nonnull Cursor cursor) {
            return new ListMapper<Chat>(new ChatMapper(chatDao)).convert(cursor);
        }
    }
}
