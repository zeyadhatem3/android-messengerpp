package org.solovyev.android.messenger.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import javax.annotation.Nonnull;

import org.solovyev.android.fragments.DetachableFragment;
import org.solovyev.android.messenger.MultiPaneManager;

import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;

/**
 * User: serso
 * Date: 8/16/12
 * Time: 2:13 AM
 */
public class EmptyFragment extends RoboSherlockFragment implements DetachableFragment {

	@Nonnull
	public static final String FRAGMENT_TAG = "empty";

	@Inject
	@Nonnull
	private MultiPaneManager multiPaneManager;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final LinearLayout root = new LinearLayout(this.getActivity());

		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		multiPaneManager.onCreatePane(this.getActivity(), container, root);

		return root;
	}
}