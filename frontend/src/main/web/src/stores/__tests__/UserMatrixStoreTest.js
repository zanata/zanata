import UserMatrixStore from '../UserMatrixStore'

/* eslint-disable no-undef */
jest.dontMock('moment')
jest.dontMock('moment-range')
jest.mock('../../utils/DateHelper')

// Get default for es6 modules
var DateHelper = require('../../utils/DateHelper')

describe('UserMatrixStore', function () {
  var MockRequest
  var baseUrl = 'http://localhost/base'
  var user = {
    'username': 'test-user',
    'email': 'zanata@zanata.org',
    'name': 'admin-name',
    'loggedIn': 'true',
    'imageUrl': '//www.gravatar.com/avatar/dda6e90e3f2a615fb8b31205e8b4894b?d=mm&r=g&s=115', // eslint-disable-line max-len
    'languageTeams': 'English, French, German, Yodish (Yoda English)'
  }
  var data = {
    'permission': {
      'updateGlossary': true,
      'insertGlossary': true
    },
    'dev': 'true',
    'profileUser': {
      'username': 'test-user',
      'email': 'zanata@zanata.org',
      'name': 'admin-name',
      'loggedIn': 'true',
      'imageUrl': '//www.gravatar.com/avatar/dda6e90e3f2a615fb8b31205e8b4894b?d=mm&r=g&s=115', // eslint-disable-line max-len
      'languageTeams': 'English, French, German, Yodish (Yoda English)'
    }
  }

  var responseBody = [
    {
      'savedDate': '2015-03-01',
      'projectSlug': 'gcc',
      'projectName': 'gcc',
      'versionSlug': 'master',
      'localeId': 'zh',
      'localeDisplayName': 'Chinese',
      'savedState': 'Translated',
      'wordCount': 100
    }]

  beforeEach(function () {
    require('../../constants/Configs').baseUrl = baseUrl
    require('../../constants/Configs').user = user
    require('../../constants/Configs').data = data
    MockRequest = require('superagent')
    MockRequest.__setResponse(
      'http://localhost/base/stats/user/test-user/2015-03-01..2015-03-07',
      {
        error: false,
        body: responseBody
      }
    )

    DateHelper
      .getDateRangeFromOption
      .mockImplementation(
        function (option) {
          return {
            fromDate: '2015-03-01',
            toDate: '2015-03-07',
            dates: [
              '2015-03-01',
              '2015-03-02',
              '2015-03-03',
              '2015-03-04',
              '2015-03-05',
              '2015-03-06',
              '2015-03-07'
            ]
          }
        }
    )
  })

  it('will load from server if state is empty', function () {
    UserMatrixStore
      .addChangeListener(function () {
        var state = UserMatrixStore.getMatrixState()

        expect(state.dateRangeOption).toEqual('This Week')
        expect(state.contentStateOption).toEqual('Total')
        expect(state.selectedDay).toBeNull()
        expect(state.matrix).toEqual(responseBody)
        expect(state.matrixForAllDays).toEqual(
          [
            {date: '2015-03-01', totalActivity: 100, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 100}, // eslint-disable-line max-len
            {date: '2015-03-02', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0}, // eslint-disable-line max-len
            {date: '2015-03-03', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0}, // eslint-disable-line max-len
            {date: '2015-03-04', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0}, // eslint-disable-line max-len
            {date: '2015-03-05', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0}, // eslint-disable-line max-len
            {date: '2015-03-06', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0}, // eslint-disable-line max-len
            {date: '2015-03-07', totalActivity: 0, totalApproved: 0, totalNeedsWork: 0, totalTranslated: 0} // eslint-disable-line max-len
          ]
        )
        expect(state.wordCountsForEachDayFilteredByContentState).toEqual(
          [
            {date: '2015-03-01', wordCount: 100},
            {date: '2015-03-02', wordCount: 0},
            {date: '2015-03-03', wordCount: 0},
            {date: '2015-03-04', wordCount: 0},
            {date: '2015-03-05', wordCount: 0},
            {date: '2015-03-06', wordCount: 0},
            {date: '2015-03-07', wordCount: 0}
          ]
        )
        expect(state.wordCountsForSelectedDayFilteredByContentState).toEqual(
          [
            {
              'savedDate': '2015-03-01',
              'projectSlug': 'gcc',
              'projectName': 'gcc',
              'versionSlug': 'master',
              'localeId': 'zh',
              'localeDisplayName': 'Chinese',
              'savedState': 'Translated',
              'wordCount': 100
            }
          ]
        )
      })
    UserMatrixStore.getMatrixState()
  })
})

/* eslint-enable */
