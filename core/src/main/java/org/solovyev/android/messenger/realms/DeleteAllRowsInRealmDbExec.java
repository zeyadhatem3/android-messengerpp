package org.solovyev.android.messenger.realms;

import android.database.sqlite.SQLiteDatabase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.solovyev.android.db.DbExec;

/**
 * User: serso
 * Date: 2/28/13
 * Time: 9:02 PM
 */
public class DeleteAllRowsInRealmDbExec implements DbExec {

    @Nonnull
    private final String tableName;

    @Nullable
    private final String realmColumnName;

    @Nullable
    private final String foreignKeyColumnName;

    @Nonnull
    private final String realmId;

    private DeleteAllRowsInRealmDbExec(@Nonnull String tableName,
                                       @Nullable String realmColumnName,
                                       @Nullable String foreignKeyColumnName,
                                       @Nonnull String realmId) {
        this.tableName = tableName;
        this.realmColumnName = realmColumnName;
        this.foreignKeyColumnName = foreignKeyColumnName;
        this.realmId = realmId;
    }

    @Nonnull
    public static DeleteAllRowsInRealmDbExec newInstance(@Nonnull String tableName, @Nonnull String realmColumnName, @Nonnull String realmId) {
        return new DeleteAllRowsInRealmDbExec(tableName, realmColumnName, null, realmId);
    }

    @Nonnull
    public static DeleteAllRowsInRealmDbExec newStartsWith(@Nonnull String tableName, @Nonnull String foreignKeyColumnName, @Nonnull String realmId) {
        return new DeleteAllRowsInRealmDbExec(tableName, null, foreignKeyColumnName, realmId);
    }


    @Override
    public void exec(@Nonnull SQLiteDatabase db) {
        if (realmColumnName != null) {
            db.delete(tableName, realmColumnName + " = ?", new String[]{realmId});
        } else if (foreignKeyColumnName != null) {
            // todo serso: use ?
            db.delete(tableName, foreignKeyColumnName + " like '" + realmId + RealmEntityImpl.DELIMITER + "%'", null);
        }
    }
}