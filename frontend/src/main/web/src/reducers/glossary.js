import { handleActions } from 'redux-actions'
import { union, isEmpty, cloneDeep, forEach, size } from 'lodash'
import {
  GLOSSARY_UPDATE_INDEX,
  GLOSSARY_UPDATE_LOCALE,
  GLOSSARY_UPDATE_FILTER,
  GLOSSARY_INIT_STATE_FROM_URL,
  GLOSSARY_TERMS_INVALIDATE,
  GLOSSARY_TERMS_REQUEST,
  GLOSSARY_TERMS_SUCCESS,
  GLOSSARY_TERMS_FAILURE,
  GLOSSARY_STATS_REQUEST,
  GLOSSARY_STATS_SUCCESS,
  GLOSSARY_STATS_FAILURE,
  GLOSSARY_SELECT_TERM,
  GLOSSARY_PAGE_SIZE,
  GLOSSARY_UPDATE_FIELD,
  GLOSSARY_RESET_TERM,
  GLOSSARY_UPDATE_REQUEST,
  GLOSSARY_UPDATE_SUCCESS,
  GLOSSARY_UPDATE_FAILURE,
  GLOSSARY_DELETE_REQUEST,
  GLOSSARY_DELETE_SUCCESS,
  GLOSSARY_DELETE_FAILURE,
  GLOSSARY_UPLOAD_REQUEST,
  GLOSSARY_UPLOAD_SUCCESS,
  GLOSSARY_UPLOAD_FAILURE,
  GLOSSARY_UPDATE_IMPORT_FILE,
  GLOSSARY_UPDATE_IMPORT_FILE_LOCALE,
  GLOSSARY_TOGGLE_IMPORT_DISPLAY,
  GLOSSARY_UPDATE_SORT,
  GLOSSARY_TOGGLE_NEW_ENTRY_DISPLAY,
  GLOSSARY_CREATE_REQUEST,
  GLOSSARY_CREATE_SUCCESS,
  GLOSSARY_CREATE_FAILURE
} from '../actions/glossary'
import {
  CLEAR_MESSAGE,
  SEVERITY,
  DEFAULT_LOCALE
} from '../actions/common'
import GlossaryHelper from '../utils/GlossaryHelper'

const glossary = handleActions({
  [CLEAR_MESSAGE]: (state, action) => {
    return {
      ...state,
      notification: null
    }
  },
  [GLOSSARY_INIT_STATE_FROM_URL]: (state, action) => {
    return {
      ...state,
      src: action.payload.src || DEFAULT_LOCALE.localeId,
      locale: action.payload.locale || '',
      filter: action.payload.filter || '',
      sort: GlossaryHelper.convertSortToObject(action.payload.sort),
      index: action.payload.index || 0,
      permission: {
        canAddNewEntry: window.config.permission.insertGlossary,
        canUpdateEntry: window.config.permission.updateGlossary,
        canDeleteEntry: window.config.permission.deleteGlossary
      }
    }
  },
  [GLOSSARY_UPDATE_INDEX]: (state, action) => ({
    ...state,
    index: action.payload
  }),
  [GLOSSARY_UPDATE_LOCALE]: (state, action) => ({
    ...state,
    selectedTerm : {},
    locale: action.payload
  }),
  [GLOSSARY_UPDATE_FILTER]: (state, action) => ({
    ...state,
    filter: action.payload
  }),
  [GLOSSARY_UPDATE_SORT]: (state, action) => {
    return {
      ...state,
      sort: action.payload
    }
  },
  [GLOSSARY_UPLOAD_REQUEST]: (state, action) => {
    if (action.error) {
      return {
        ...state,
        notification: {
          severity: SEVERITY.ERROR,
          message:
            'We were unable to import your file. ' +
            'Please refresh this page and try again.'
        }
      }
    } else {
      let importFile = state.importFile
      importFile.status = 0
      return {
        ...state,
        importFile: importFile
      }
    }
  },
  [GLOSSARY_UPLOAD_SUCCESS]: (state, action) => ({
    ...state,
    importFile: {
      show: false,
      status: -1,
      file: null,
      transLocale: null
    },
    notification: {
      severity: SEVERITY.INFO,
      message: 'File imported successfully',
      details: size(action.payload.glossaryEntries) + ' terms imported.'
    }
  }),
  [GLOSSARY_UPLOAD_FAILURE]: (state, action) => ({
    ...state,
    importFile: {
      show: false,
      status: -1,
      file: null,
      transLocale: null
    },
    notification: {
      severity: SEVERITY.ERROR,
      message:
        'We were unable to import your file. ' +
        'Please refresh this page and try again.'
    }
  }),
  [GLOSSARY_UPDATE_IMPORT_FILE]: (state, action) => {
    return {
      ...state,
      importFile: {
        ...state.importFile,
        file: action.payload
      }
    }
  },
  [GLOSSARY_UPDATE_IMPORT_FILE_LOCALE]: (state, action) => {
    return {
      ...state,
      importFile: {
        ...state.importFile,
        transLocale: action.payload
      }
    }
  },
  [GLOSSARY_TOGGLE_IMPORT_DISPLAY]: (state, action) => {
    return {
      ...state,
      importFile: {
        ...state.importFile,
        show: action.payload
      }
    }
  },
  [GLOSSARY_TOGGLE_NEW_ENTRY_DISPLAY]: (state, action) => {
    return {
      ...state,
      newEntry: {
        ...state.newEntry,
        show: action.payload
      }
    }
  },
  [GLOSSARY_RESET_TERM]: (state, action) => {
    return {
      ...state,
      selectedTerm: cloneDeep(state.terms[action.payload])
    }
  },
  [GLOSSARY_UPDATE_FIELD]: (state, action) => {
    let newSelectedTerm = cloneDeep(state.selectedTerm)
    switch (action.payload.field) {
      case 'src':
        newSelectedTerm.srcTerm.content = action.payload.value
        break
      case 'locale':
        if (newSelectedTerm.transTerm) {
          newSelectedTerm.transTerm.content = action.payload.value
        } else {
          newSelectedTerm.transTerm =
            GlossaryHelper.generateEmptyTerm(state.locale)
          newSelectedTerm.transTerm.content = action.payload.value
        }
        if (isEmpty(newSelectedTerm.transTerm.content)) {
          newSelectedTerm.transTerm.comment = null
        }
        break
      case 'pos':
        newSelectedTerm.pos = action.payload.value
        break
      case 'description':
        newSelectedTerm.description = action.payload.value
        break
      case 'comment':
        if (newSelectedTerm.transTerm) {
          newSelectedTerm.transTerm.comment = action.payload.value
        } else {
          console.error('comment not allow for empty translation')
        }
        break
      default: console.error('Not a valid field')
    }
    newSelectedTerm.status = GlossaryHelper.getEntryStatus(
      newSelectedTerm, state.terms[newSelectedTerm.id])
    return {
      ...state,
      selectedTerm: newSelectedTerm
    }
  },
  [GLOSSARY_STATS_REQUEST]: (state, action) => {
    if (action.error) {
      return {
        ...state,
        notification: {
          severity: SEVERITY.ERROR,
          message: 'We are unable to get information from server. ' +
          'Please refresh this page and try again.'
        }
      }
    } else {
      return {
        ...state,
        statsError: false,
        statsLoading: true
      }
    }
  },
  [GLOSSARY_STATS_SUCCESS]: (state, action) => {
    return ({
      ...state,
      stats: {
        srcLocale: action.payload.srcLocale,
        transLocales: action.payload.transLocale.map(result => ({
          value: result.locale.localeId,
          label: result.locale.displayName,
          count: result.numberOfTerms
        }))
      },
      statsLoading: false
    })
  },
  [GLOSSARY_STATS_FAILURE]: (state, action) => ({
    ...state,
    statsError: true,
    statsErrorMessage: action.payload,
    statsLoading: false,
    notification: {
      severity: SEVERITY.ERROR,
      message: 'We are unable to get information from server. ' +
      'Please refresh this page and try again.'
    }
  }),
  [GLOSSARY_TERMS_INVALIDATE]: (state, action) => ({
    ...state,
    termsDidInvalidate: true
  }),
  [GLOSSARY_DELETE_REQUEST]: (state, action) => {
    if (action.error) {
      return {
        ...state,
        notification: {
          severity: SEVERITY.ERROR,
          message:
          'We were unable to delete the glossary term. ' +
          'Please refresh this page and try again.'
        }
      }
    }

    const entryId = action.payload
    return {
      ...state,
      deleting: {
        ...state.deleting,
        [entryId]: entryId
      }
    }
  },
  [GLOSSARY_DELETE_SUCCESS]: (state, action) => {
    let deleting = cloneDeep(state.deleting)
    const entryId = action.payload.id
    delete deleting[entryId]
    return {
      ...state,
      deleting: deleting
    }
  },
  [GLOSSARY_DELETE_FAILURE]: (state, action) => ({
    ...state,
    notification: {
      severity: SEVERITY.ERROR,
      message:
        'We were unable to delete the glossary term. ' +
        'Please refresh this page and try again.'
    }
  }),

  [GLOSSARY_UPDATE_REQUEST]: (state, action) => {
    if (action.error) {
      return {
        ...state,
        notification: {
          severity: SEVERITY.ERROR,
          message:
          'We were unable to update the glossary term. ' +
          'Please refresh this page and try again.'
        }
      }
    } else {
      let saving = cloneDeep(state.saving)
      const entryId = action.payload.id
      saving[entryId] = cloneDeep(action.payload)
      return {
        ...state,
        saving: saving
      }
    }
  },
  [GLOSSARY_UPDATE_SUCCESS]: (state, action) => {
    let saving = cloneDeep(state.saving)
    let selectedTerm = state.selectedTerm
    let terms = cloneDeep(state.terms)
    forEach(action.payload.glossaryEntries, (rawEntry) => {
      const entry = GlossaryHelper.generateEntry(rawEntry, state.locale)
      terms[rawEntry.id] = entry

      if (selectedTerm && selectedTerm.id === rawEntry.id) {
        selectedTerm = cloneDeep(entry)
      }
      delete saving[entry.id]
    })

    return {
      ...state,
      saving: saving,
      terms: terms,
      selectedTerm: selectedTerm
    }
  },
  [GLOSSARY_UPDATE_FAILURE]: (state, action) => {
    return {
      ...state,
      notification: {
        severity: SEVERITY.ERROR,
        message:
          'We were unable to update the glossary term. ' +
          'Please refresh this page and try again.'
      }
    }
  },
  [GLOSSARY_CREATE_REQUEST]: (state, action) => {
    if (action.error) {
      return {
        ...state,
        notification: {
          severity: SEVERITY.ERROR,
          message:
          'We were unable save glossary entry. ' +
          'Please refresh this page and try again.'
        }
      }
    } else {
      const newEntry = state.newEntry
      return {
        ...state,
        newEntry: {
          ...newEntry,
          isSaving: true
        }
      }
    }
  },
  [GLOSSARY_CREATE_SUCCESS]: (state, action) => {
    const newEntry = state.newEntry
    return {
      ...state,
      newEntry: {
        ...newEntry,
        isSaving: false,
        entry: GlossaryHelper.generateEmptyEntry(state.src),
        show: false
      },
      notification: {
        severity: SEVERITY.INFO,
        message: 'Glossary term created.'
      }
    }
  },
  [GLOSSARY_CREATE_FAILURE]: (state, action) => {
    const newEntry = state.newEntry
    return {
      ...state,
      newEntry: {
        ...newEntry,
        isSaving: false,
        entry: GlossaryHelper.generateEmptyEntry(state.src),
        show: false
      },
      notification: {
        severity: SEVERITY.ERROR,
        message:
        'We were unable save glossary entry. ' +
        'Please refresh this page and try again.'
      }
    }
  },
  [GLOSSARY_TERMS_REQUEST]: (state, action) => {
    if (action.error) {
      return state
    } else {
      return {
        ...state,
        termsError: action.error,
        termsErrorMessage: action.payload,
        termsLoading: true
      }
    }
  },
  [GLOSSARY_TERMS_SUCCESS]: (state, action) => {
    const page = action.meta.page
    const pagesLoaded = union(state.pagesLoaded, [page])
    let termIds = isEmpty(state.termIds)
      ? new Array(action.payload.result.totalCount)
      : cloneDeep(state.termIds)

    let entries = {}
    forEach(action.payload.entities.glossaryTerms, (entry) => {
      entries[entry.id] = GlossaryHelper.generateEntry(entry, state.locale)
    })
    const terms = isEmpty(state.terms)
      ? entries
      : { ...state.terms, ...entries }
    termIds
      .splice(
        (page - 1) * GLOSSARY_PAGE_SIZE,
        action.payload.result.results.length,
        ...action.payload.result.results
      )

    return {
      ...state,
      termsLoading: false,
      termsLastUpdated: action.meta.receivedAt,
      terms,
      termIds,
      termCount: action.payload.result.totalCount,
      page,
      pagesLoaded
    }
  },
  [GLOSSARY_TERMS_FAILURE]: (state, action) => ({
    ...state,
    termsError: action.error,
    termsErrorMessage: action.payload,
    termsLoading: false
  }),
  [GLOSSARY_SELECT_TERM]: (state, action) => {
    let selectedTerm = cloneDeep(state.terms[action.payload])
    return {
      ...state,
      selectedTerm: selectedTerm
    }
  }
},
// default state
{
  src: DEFAULT_LOCALE.localeId,
  locale: '',
  filter: '',
  sort: {
    src_content: true
  },
  index: 0,
  selectedTerm: {},
  page: 1,
  pagesLoaded: [],
  permission: {
    canAddNewEntry: false,
    canUpdateEntry: false,
    canDeleteEntry: false
  },
  terms: {},
  termIds: [],
  termCount: 0,
  termsError: false,
  termsLoading: true,
  termsDidInvalidate: false,
  stats: {
    srcLocale: {},
    transLocales: []
  },
  saving: {},
  deleting: {},
  importFile: {
    show: false,
    status: -1,
    file: null,
    transLocale: null
  },
  newEntry: {
    show: false,
    isSaving: false,
    entry: GlossaryHelper.generateEmptyEntry(DEFAULT_LOCALE.localeId)
  },
  statsError: false,
  statsLoading: true
})

export default glossary
