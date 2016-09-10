import moment from 'moment'
// this import MUST come after import for moment
import 'moment-range'
import { isEmpty } from 'lodash'

var DateHelper = {
  dateFormat: 'YYYY-MM-DD',
  getDateRangeFromOption: function (dateRange) {
    const dateFormat = this.dateFormat
    const fromDate = dateRange.startDate
    const toDate = dateRange.endDate
    var dates = []
    const range = moment.range(fromDate, toDate)

    range.by('days', function (moment) {
      dates.push(moment.format(dateFormat))
    })

    return {
      fromDate: fromDate.format(dateFormat),
      toDate: toDate.format(dateFormat),
      dates: dates
    }
  },

  dayAsLabel: function (dateStr, numOfDays, useFullName) {
    var date = moment(dateStr)
    var dayOfWeekFmt
    var dayOfMonthFmt

    dayOfWeekFmt = useFullName ? 'dddd (Do MMM)' : 'ddd'
    dayOfMonthFmt = useFullName ? 'Do MMM (dddd)' : 'D'
    if (numOfDays < 8) {
      return date.format(dayOfWeekFmt)
    } else {
      return date.format(dayOfMonthFmt)
    }
  },

  isInFuture: function (dateStr) {
    return moment(dateStr).isAfter(moment())
  },

  getDate: function (milliseconds) {
    if (!isEmpty(milliseconds)) {
      const intMiliseconds = parseInt(milliseconds)
      return new Date(intMiliseconds)
    } else {
      return undefined
    }
  },

  shortDate: function (date) {
    if (date) {
      return moment(date).format('DD/MM/YYYY')
    } else {
      return undefined
    }
  },

  /**
   * Calculate days range between startDate and endDate,
   * if more than given days, it will adjust the endDate
   *
   * @days - days different to check. Must be 0 or more.
   * returns {startDate: startDate, endDate: adjustedEndDate}
   */
  keepDateInRange: function (startDate, endDate, days) {
    if (days < 0) {
      console.error('Days must be more 0 or more.', days)
    }
    const range = moment.range(startDate, endDate)
    const adjustedEndDate = range.diff('days') >= days
      ? moment(startDate).days(startDate.days() + (days - 1))
      : endDate
    return {
      startDate,
      endDate: adjustedEndDate
    }
  },

  getDefaultDateRange: function () {
    return {
      'This week': {
        startDate: function startDate (now) {
          return moment().weekday(0)
        },
        endDate: function endDate (now) {
          return moment().weekday(6)
        }
      },
      'Last week': {
        startDate: function startDate (now) {
          return moment().weekday(-7)
        },
        endDate: function endDate (now) {
          return moment().weekday(-1)
        }
      },
      'This month': {
        startDate: function startDate (now) {
          return moment().date(1)
        },
        endDate: function endDate (now) {
          return moment().month(now.month() + 1).date(0)
        }
      },
      'Last month': {
        startDate: function startDate (now) {
          return moment().month(now.month() - 1).date(1)
        },
        endDate: function endDate (now) {
          return moment().date(0)
        }
      },
      'This year': {
        startDate: function startDate (now) {
          return moment().year(now.year()).month(0).date(1)
        },
        endDate: function endDate (now) {
          return moment().year(now.year()).month(11).date(31)
        }
      }
    }
  },

  getDateRange: function (option) {
    const dateRange = this.getDefaultDateRange()[option]
    return {
      startDate: dateRange.startDate(),
      endDate: dateRange.endDate()
    }
  }
}

export default DateHelper
