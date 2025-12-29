package com.fastcal.config;

import com.fastcal.config.converter.EmailReadingConverter;
import com.fastcal.config.converter.EmailWritingConverter;
import com.fastcal.config.converter.HashedPasswordReadingConverter;
import com.fastcal.config.converter.HashedPasswordWritingConverter;
import com.fastcal.config.converter.UserDisplayNameReadingConverter;
import com.fastcal.config.converter.UserDisplayNameWritingConverter;
import com.fastcal.config.converter.CalendarColorReadingConverter;
import com.fastcal.config.converter.CalendarColorWritingConverter;
import com.fastcal.config.converter.CalendarDescriptionReadingConverter;
import com.fastcal.config.converter.CalendarDescriptionWritingConverter;
import com.fastcal.config.converter.CalendarDisplayNameReadingConverter;
import com.fastcal.config.converter.CalendarDisplayNameWritingConverter;
import com.fastcal.config.converter.CalendarIdReadingConverter;
import com.fastcal.config.converter.CalendarIdWritingConverter;
import com.fastcal.config.converter.CalendarTimezoneReadingConverter;
import com.fastcal.config.converter.CalendarTimezoneWritingConverter;
import com.fastcal.config.converter.CTagReadingConverter;
import com.fastcal.config.converter.CTagWritingConverter;
import com.fastcal.config.converter.ETagReadingConverter;
import com.fastcal.config.converter.ETagWritingConverter;
import com.fastcal.config.converter.EventDescriptionReadingConverter;
import com.fastcal.config.converter.EventDescriptionWritingConverter;
import com.fastcal.config.converter.EventLocationReadingConverter;
import com.fastcal.config.converter.EventLocationWritingConverter;
import com.fastcal.config.converter.EventSummaryReadingConverter;
import com.fastcal.config.converter.EventSummaryWritingConverter;
import com.fastcal.config.converter.EventUidReadingConverter;
import com.fastcal.config.converter.EventUidWritingConverter;
import com.fastcal.config.converter.ICalDataReadingConverter;
import com.fastcal.config.converter.ICalDataWritingConverter;
import com.fastcal.config.converter.RecurrenceRuleReadingConverter;
import com.fastcal.config.converter.RecurrenceRuleWritingConverter;
import com.fastcal.config.converter.SyncTokenReadingConverter;
import com.fastcal.config.converter.SyncTokenWritingConverter;
import com.fastcal.config.converter.UserIdReadingConverter;
import com.fastcal.config.converter.UserIdWritingConverter;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.fastcal.domain.repository")
@EnableR2dbcAuditing
@EnableTransactionManagement
public class R2dbcConfig {

  @Bean
  public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
    return new R2dbcTransactionManager(connectionFactory);
  }

  @Bean
  public R2dbcCustomConversions r2dbcCustomConversions() {
    return R2dbcCustomConversions.of(
        PostgresDialect.INSTANCE,
        List.of(
            new EmailReadingConverter(),
            new EmailWritingConverter(),
            new HashedPasswordReadingConverter(),
            new HashedPasswordWritingConverter(),
            new UserDisplayNameReadingConverter(),
            new UserDisplayNameWritingConverter(),
            new UserIdReadingConverter(),
            new UserIdWritingConverter(),
            new CalendarIdReadingConverter(),
            new CalendarIdWritingConverter(),
            new CalendarDisplayNameReadingConverter(),
            new CalendarDisplayNameWritingConverter(),
            new CalendarDescriptionReadingConverter(),
            new CalendarDescriptionWritingConverter(),
            new CalendarColorReadingConverter(),
            new CalendarColorWritingConverter(),
            new CalendarTimezoneReadingConverter(),
            new CalendarTimezoneWritingConverter(),
            new CTagReadingConverter(),
            new CTagWritingConverter(),
            new SyncTokenReadingConverter(),
            new SyncTokenWritingConverter(),
            new EventUidReadingConverter(),
            new EventUidWritingConverter(),
            new ETagReadingConverter(),
            new ETagWritingConverter(),
            new ICalDataReadingConverter(),
            new ICalDataWritingConverter(),
            new EventSummaryReadingConverter(),
            new EventSummaryWritingConverter(),
            new EventDescriptionReadingConverter(),
            new EventDescriptionWritingConverter(),
            new EventLocationReadingConverter(),
            new EventLocationWritingConverter(),
            new RecurrenceRuleReadingConverter(),
            new RecurrenceRuleWritingConverter()
        )
    );
  }
}
