package org.zanata.webtrans.client.ui;

import org.zanata.webtrans.client.resources.Resources;
import org.zanata.webtrans.client.resources.WebTransMessages;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class Pager extends Composite implements HasPager
{

   private static PagerUiBinder uiBinder = GWT.create(PagerUiBinder.class);

   interface PagerUiBinder extends UiBinder<Widget, Pager>
   {
   }

   interface Styles extends CssResource
   {
      String enabled();

      String disabled();
   }

   @UiField
   InlineLabel firstPage, lastPage, nextPage, prevPage;

   @UiField
   TextBox gotoPage;

   @UiField
   Label pageCountLabel;

   @UiField(provided = true)
   Resources resources;

   @UiField
   Styles style;

   private int pageCount = PAGECOUNT_UNKNOWN;
   private int currentPage;
   private boolean isFocused;

   public static final int PAGECOUNT_UNKNOWN = -1;

   public Pager(final WebTransMessages messages, final Resources resources)
   {
      this.resources = resources;
      initWidget(uiBinder.createAndBindUi(this));

      // set tooltips of page nav icons, i18n-ized w/ WebTransMessages.java
      firstPage.setTitle(messages.firstPage());
      prevPage.setTitle(messages.prevPage());
      nextPage.setTitle(messages.nextPage());
      lastPage.setTitle(messages.lastPage());
   }

   @UiHandler("gotoPage")
   public void onGotoPageFocus(FocusEvent event)
   {
      isFocused = true;
   }

   @UiHandler("gotoPage")
   public void onGotoPageBlur(BlurEvent event)
   {
      isFocused = false;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      gotoPage.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
            {
               try
               {
                  int newValue = Integer.parseInt(gotoPage.getText());
                  setValue(newValue);
               }
               catch (NumberFormatException nfe)
               {
               }
            }
         }
      });

      firstPage.addClickHandler(clickHandler);
      lastPage.addClickHandler(clickHandler);
      prevPage.addClickHandler(clickHandler);
      nextPage.addClickHandler(clickHandler);
      refresh();
   }

   private void refresh()
   {
      String page = pageCount == PAGECOUNT_UNKNOWN ? "" : "of " + pageCount;
      pageCountLabel.setText(page);
      setEnabled(firstPage, currentPage != 1);
      setEnabled(prevPage, currentPage != 1);
      setEnabled(nextPage, currentPage != pageCount);
      setEnabled(lastPage, currentPage != pageCount && pageCount != PAGECOUNT_UNKNOWN);

      gotoPage.setText(String.valueOf(currentPage));
   }

   @Override
   public void setPageCount(int pageCount)
   {
      this.pageCount = pageCount;
      refresh();
   }

   @Override
   public int getPageCount()
   {
      return pageCount;
   }

   @Override
   public Integer getValue()
   {
      return currentPage;
   }

   @Override
   public void setValue(Integer value)
   {
      setValue(value, true);
   }

   @Override
   public void setValue(Integer value, boolean fireEvents)
   {
      if (value != this.currentPage && (value > 0 && value <= pageCount))
      {
         this.currentPage = value;
         if (fireEvents)
         {
            ValueChangeEvent.fire(this, value);
         }
         refresh();
      }
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   private final ClickHandler clickHandler = new ClickHandler()
   {

      @Override
      public void onClick(ClickEvent event)
      {
         if (event.getSource() == firstPage)
         {
            if (isButtonEnabled(firstPage))
            {
               setValue(1);
            }
         }
         else if (event.getSource() == lastPage)
         {
            if (isButtonEnabled(lastPage))
            {
               setValue(pageCount);
            }
         }
         else if (event.getSource() == nextPage)
         {
            if (isButtonEnabled(nextPage))
            {
               setValue(currentPage + 1);
            }
         }
         else if (event.getSource() == prevPage)
         {
            if (isButtonEnabled(prevPage))
            {
               setValue(currentPage - 1);
            }
         }
      }
   };

   private boolean isButtonEnabled(InlineLabel button)
   {
      return button.getStyleName().contains(style.enabled());
   }

   @Override
   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return gotoPage.addFocusHandler(handler);
   }

   @Override
   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return gotoPage.addBlurHandler(handler);
   }

   private void setEnabled(InlineLabel button, boolean enabled)
   {
      if (enabled)
      {
         button.removeStyleName(style.disabled());
         button.addStyleName(style.enabled());
      }
      else
      {
         button.removeStyleName(style.enabled());
         button.addStyleName(style.disabled());
      }
   }

   public boolean isFocused()
   {
      return isFocused;
   }
}
