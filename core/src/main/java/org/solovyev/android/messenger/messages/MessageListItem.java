/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.solovyev.android.messenger.messages;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.ClipboardManager;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.solovyev.android.list.ListItemOnClickData;
import org.solovyev.android.list.SimpleMenuOnClick;
import org.solovyev.android.menu.LabeledMenuItem;
import org.solovyev.android.messenger.App;
import org.solovyev.android.messenger.accounts.Account;
import org.solovyev.android.messenger.chats.Chat;
import org.solovyev.android.messenger.chats.ChatUiEvent;
import org.solovyev.android.messenger.core.R;
import org.solovyev.android.messenger.entities.Entity;
import org.solovyev.android.messenger.realms.Realm;
import org.solovyev.android.messenger.users.ContactUiEvent;
import org.solovyev.android.messenger.users.User;
import org.solovyev.android.messenger.view.BaseMessengerListItem;
import org.solovyev.android.messenger.view.ViewAwareTag;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static android.text.Html.fromHtml;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.solovyev.android.messenger.App.getEventManager;
import static org.solovyev.android.messenger.App.showToast;

public final class MessageListItem extends BaseMessengerListItem<Message> /*, ChatEventListener*/ {

	@Nonnull
	private static final String TAG_PREFIX = "message_list_item_";

	@Nonnull
	private static final MessageLayout layout = MessageLayout.wrap_content;

	@Nonnull
	private static final MessageStyle userStyle = MessageStyle.light_grey;

	@Nonnull
	private static final MessageStyle contactStyle = MessageStyle.light_grey;

	@Nonnull
	private final Account account;

	@Nonnull
	private Chat chat;

	private final boolean userMessage;

	@Nonnull
	private final IconOnClickListener iconOnClickListener = new IconOnClickListener();

	private MessageListItem(@Nonnull Account account,
							@Nonnull Chat chat,
							@Nonnull Message message,
							boolean userMessage) {
		super(TAG_PREFIX + (userMessage ? "0_" : "1_"), message, layout.getLayoutResId(userMessage), false);
		this.account = account;
		this.chat = chat;
		this.userMessage = userMessage;
	}

	@Nonnull
	public static MessageListItem newMessageListItem(@Nonnull Account account, @Nonnull Chat chat, @Nonnull Message message) {
		final boolean userMessage = account.getUser().getEntity().equals(message.getAuthor());
		return new MessageListItem(account, chat, message, userMessage);
	}

	@Override
	public OnClickAction getOnClickAction() {
		return new SimpleMenuOnClick<MessageListItem>(Arrays.<LabeledMenuItem<ListItemOnClickData<MessageListItem>>>asList(MenuItems.values()), this, "message-context-menu");
	}

	@Override
	public OnClickAction getOnLongClickAction() {
		return null;
	}

	@Nonnull
	@Override
	protected CharSequence getDisplayName(@Nonnull Message data, @Nonnull Context context) {
		return data.getBody();
	}

	public boolean isUserMessage() {
		return userMessage;
	}

	@Override
	protected void fillView(@Nonnull Message message, @Nonnull Context context, @Nonnull ViewAwareTag viewTag) {
		final TextView messageTextView = viewTag.getViewById(R.id.mpp_li_message_body_textview);

		final TextView messageDate = viewTag.getViewById(R.id.mpp_li_message_date_textview);
		final ImageView messageProgress = viewTag.getViewById(R.id.mpp_li_message_progress_imageview);
		final AnimationDrawable animation = (AnimationDrawable) messageProgress.getDrawable();

		if (message.getState() == MessageState.sending) {
			messageDate.setVisibility(GONE);
			messageProgress.setVisibility(VISIBLE);
			messageDate.setText(null);
			animation.start();
		} else {
			messageDate.setVisibility(VISIBLE);
			messageProgress.setVisibility(GONE);
			messageDate.setText(Messages.getMessageTime(message));
			animation.stop();
		}

		String messageBody = message.getBody();

		final Realm realm = account.getRealm();
		if (!realm.isHtmlMessage()) {
			messageBody = messageBody.replace("\n", "<br>");
		}

		final ImageView messageIcon = viewTag.getViewById(R.id.mpp_li_message_icon_imageview);
		messageIcon.setOnClickListener(iconOnClickListener);
		iconOnClickListener.message = message;
		iconOnClickListener.context = context;
		App.getMessageService().setMessageIcon(message, messageIcon);

		messageTextView.setText(fromHtml(messageBody));
		Linkify.addLinks(messageTextView, Linkify.ALL);

		getStyle().prepareLayout(userMessage, true, viewTag);

		if (message.canRead()) {
			final Message readMessage = message.cloneRead();
			setData(readMessage);
			getEventManager(context).fire(new ChatUiEvent.MarkMessageRead(chat, readMessage));
		}
	}

	@Nonnull
	private MessageStyle getStyle() {
		return userMessage ? userStyle : contactStyle;
	}

	void onMessageChanged(@Nonnull Message message) {
		setData(message);
	}

	public static final class Comparator implements java.util.Comparator<MessageListItem> {

		@Nonnull
		private static final Comparator instance = new Comparator();

		private Comparator() {
		}

		@Nonnull
		public static Comparator getInstance() {
			return instance;
		}

		@Override
		public int compare(@Nonnull MessageListItem lhs, @Nonnull MessageListItem rhs) {
			return lhs.getData().getSendDate().compareTo(rhs.getData().getSendDate());
		}
	}

	private static enum MenuItems implements LabeledMenuItem<ListItemOnClickData<MessageListItem>> {

		copy(R.string.mpp_copy) {
			@Override
			public void onClick(@Nonnull ListItemOnClickData<MessageListItem> data, @Nonnull Context context) {
				final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);

				final MessageListItem messageListItem = data.getDataObject();
				clipboard.setText(messageListItem.getData().getBody());

				showToast(R.string.mpp_message_copied_to_clipboard);
			}
		},

		quote(R.string.mpp_quote) {
			@Override
			public void onClick(@Nonnull ListItemOnClickData<MessageListItem> data, @Nonnull Context context) {
				getEventManager(context).fire(MessageUiEventType.quote.newEvent(data.getDataObject().getMessage()));
			}
		},

		remove(R.string.mpp_remove) {
			@Override
			public void onClick(@Nonnull ListItemOnClickData<MessageListItem> data, @Nonnull Context context) {
				final MessageListItem listItem = data.getDataObject();
				final Message message = listItem.getData();
				App.getChatService().removeMessage(message);
			}
		};

		private int captionResId;

		private MenuItems(int captionResId) {
			this.captionResId = captionResId;
		}

		@Nonnull
		@Override
		public String getCaption(@Nonnull Context context) {
			return context.getString(captionResId);
		}
	}

	@Nonnull
	public Message getMessage() {
		return getData();
	}

	private static class IconOnClickListener implements View.OnClickListener {

		@Nonnull
		private Message message;

		@Nonnull
		private Context context;

		@Override
		public void onClick(View v) {
			final Entity author = message.getAuthor();
			final User user = App.getUserService().getUserById(author);
			getEventManager(context).fire(new ContactUiEvent.Open(user));
		}
	}
}
