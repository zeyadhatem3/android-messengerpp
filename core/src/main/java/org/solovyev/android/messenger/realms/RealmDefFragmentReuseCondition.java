package org.solovyev.android.messenger.realms;

import android.support.v4.app.Fragment;
import org.solovyev.common.JPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* User: serso
* Date: 3/7/13
* Time: 9:32 PM
*/
public class RealmDefFragmentReuseCondition implements JPredicate<Fragment> {

    @Nonnull
    private final RealmDef realmDef;

    public RealmDefFragmentReuseCondition(@Nonnull RealmDef realmDef) {
        this.realmDef = realmDef;
    }

    @Override
    public boolean apply(@Nullable Fragment fragment) {
        if (fragment instanceof BaseRealmConfigurationFragment) {
            final BaseRealmConfigurationFragment oldRealmFragment = ((BaseRealmConfigurationFragment) fragment);
            if (realmDef.equals(oldRealmFragment.getRealmDef())) {
                // do nothing - configuration fragment for this item is already opened
                return true;
            }
        }

        return false;
    }
}