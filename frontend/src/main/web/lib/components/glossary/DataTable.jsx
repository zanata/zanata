import React, {PureRenderMixin} from 'react/addons';
import Actions from '../../actions/GlossaryActions';
import {Table, Column} from 'fixed-data-table';
import StringUtils from '../../utils/StringUtils'
import InputCell from './InputCell';
import LoadingCell from './LoadingCell'
import ActionCell from './ActionCell'
import SourceActionCell from './SourceActionCell'
import ColumnHeader from './ColumnHeader'
import _ from 'lodash';

var DataTable = React.createClass({
  TIMEOUT: 400,

  NO_ROW: -1,

  resIdIndex: 0,

  ENTRY: {
    SRC: {
      col: 1,
      field: 'srcTerm.content',
      sort_field: 'src_content'
    },
    TRANS: {
      col: 2,
      field: 'transTerm.content',
      sort_field: 'trans_content'
    },
    POS: {
      col: 3,
      field: 'pos',
      sort_field: 'part_of_speech'
    },
    DESC: {
      col: 4,
      field: 'description',
      sort_field: 'desc'
    },
    TRANS_COUNT: {
      col: 5,
      field: 'termsCount',
      sort_field: 'trans_count'
    }
  },
  CELL_HEIGHT: 48,

  propTypes: {
    glossaryData: React.PropTypes.object.isRequired,
    glossaryResId: React.PropTypes.arrayOf(
      React.PropTypes.arrayOf(React.PropTypes.string)
    ),
    canAddNewEntry: React.PropTypes.bool.isRequired,
    canUpdateEntry: React.PropTypes.bool.isRequired,
    user: React.PropTypes.shape({
      username: React.PropTypes.string,
      email: React.PropTypes.string,
      name: React.PropTypes.string,
      imageUrl: React.PropTypes.string,
      languageTeams: React.PropTypes.string
    }),
    srcLocale: React.PropTypes.shape({
      locale: React.PropTypes.shape({
        localeId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired,
        alias: React.PropTypes.string.isRequired
      }).isRequired,
      numberOfTerms: React.PropTypes.number.isRequired
    }),
    selectedTransLocale: React.PropTypes.string,
    totalCount: React.PropTypes.number.isRequired,
    focusedRow: React.PropTypes.shape({
      resId: React.PropTypes.string,
      rowIndex: React.PropTypes.number
    })
  },

  getInitialState: function () {
    var top = 246; //top height for banner if can't get height from dom
    return {
      tbl_width: this._getWidth(),
      tbl_height: this._getHeight(top),
      row_height: this.CELL_HEIGHT,
      header_height: this.CELL_HEIGHT,
      hoveredRow: -1
    }
  },

  /**
   *
   * @param fixedTop - top height for banner if can't get height from dom
   */
  _getHeight: function(fixedTop) {
    var footer = window.document.getElementById("footer");
    var footerHeight = footer ? footer.clientHeight : 91;
    var top = _.isUndefined(fixedTop) ? React.findDOMNode(this).offsetTop: fixedTop;
    var newHeight = window.innerHeight - footerHeight - top;

    //minimum height 250px
    return Math.max(newHeight, 250);
  },

  _getWidth: function () {
    return window.innerWidth - 48;
  },

  _handleResize: function(e) {
    this.setState({tbl_height: this._getHeight(), tbl_width: this._getWidth()});
  },

  componentDidMount: function() {
    window.addEventListener('resize', this._handleResize);
  },

  componentWillUnmount: function() {
    window.removeEventListener('resize', this._handleResize);
  },

  _generateTermInfo: function(term) {
    var title = "";
    if(!_.isUndefined(term) && !_.isNull(term)) {
      if (!StringUtils.isEmptyOrNull(term.lastModifiedBy)
        || !StringUtils.isEmptyOrNull(term.lastModifiedDate)) {
        const parts = ['Last updated'];
        if (!StringUtils.isEmptyOrNull(term.lastModifiedBy)) {
          parts.push('by: ');
          parts.push(term.lastModifiedBy);
        }
        if (!StringUtils.isEmptyOrNull(term.lastModifiedDate)) {
          parts.push(term.lastModifiedDate);
        }
        title = parts.join(' ');
      }
    }
    if(StringUtils.isEmptyOrNull(title)) {
      title = "No information available";
    }
    return title;
  },

  _generateKey: function (colIndex, rowIndex, resId) {
    var key = colIndex + ":" + rowIndex + ":" + resId;
    if(this.props.selectedTransLocale) {
      key += ":" + this.props.selectedTransLocale;
    }
    return key;
  },

  _getSort: function (key) {
    if(_.isUndefined(this.props.sort[key])) {
      return null;
    } else if(this.props.sort[key]) {
      return "ascending";
    } else {
      return "descending";
    }
  },

  _renderSourceHeader: function (label) {
    var key = this.ENTRY.SRC.sort_field,
      asc = this._getSort(key);
    return this._renderHeader(label, key, asc, true);
  },

  _renderTransHeader: function (label) {
    var key = this.ENTRY.TRANS.sort_field,
      asc = null;
    return this._renderHeader(label, key, asc, false);
  },

  _renderPosHeader: function (label) {
    var key = this.ENTRY.POS.sort_field,
      asc = this._getSort(key);
    return this._renderHeader(label, key, asc, true);
  },

  _renderDescHeader: function (label) {
    var key = this.ENTRY.DESC.sort_field,
      asc = this._getSort(key);
    return this._renderHeader(label, key, asc, false);
  },

  _renderTransCountHeader: function (label) {
    var key = this.ENTRY.TRANS_COUNT.sort_field,
      asc = this._getSort(key);
    return this._renderHeader(label, key, asc, true);
  },

  _renderHeader: function (label, key, asc, allowSort) {
    return (
      <ColumnHeader
        value={label}
        field={key}
        key={key}
        allowSort={allowSort}
        sort={asc}
        onClickCallback={this._onHeaderClick}/>
    );
  },

  _onHeaderClick: function (field, ascending) {
    Actions.updateSortOrder(field, ascending);
  },

  _renderCell: function (cellData) {
    var key = this._generateKey(cellData.field.col, cellData.rowIndex, cellData.resId);
    if (cellData.resId === null) {
      return (<LoadingCell key={key}/>)
    } else {
      var entry = this._getGlossaryEntry(cellData.resId);
      var value = _.get(entry, cellData.field.field);
      if (cellData.readOnly) {
        return (<span className="mh1/2" key={key}>{value}</span>)
      } else {
        return (<InputCell
          value={value}
          resId={cellData.resId}
          key={key}
          placeholder={cellData.placeholder}
          rowIndex={cellData.rowIndex}
          field={cellData.field.field}
          onFocusCallback={this._onRowClick}/>);
      }
    }
  },

  _renderSourceCell: function (resId, cellDataKey, rowData, rowIndex,
                               columnData, width) {
    return this._renderCell({
      resId: resId,
      rowIndex: rowIndex,
      field: this.ENTRY.SRC,
      readOnly: true,
      placeholder: ''
    });
  },

  _renderTransCell: function(resId, cellDataKey, rowData, rowIndex,
                             columnData, width) {
    var readOnly = !this.props.canUpdateEntry,
      placeholder = 'enter a translation';
    return this._renderCell({
      resId: resId,
      rowIndex: rowIndex,
      field: this.ENTRY.TRANS,
      readOnly: readOnly,
      placeholder: placeholder
    });
  },

  _renderPosCell: function (resId, cellDataKey, rowData, rowIndex,
                            columnData, width) {
    var readOnly = !this.props.canUpdateEntry || this._isTranslationSelected(),
      placeholder = 'enter part of speech';
    return this._renderCell({
      resId: resId,
      rowIndex: rowIndex,
      field: this.ENTRY.POS,
      readOnly: readOnly,
      placeholder: placeholder
    });
  },

  _renderDescCell: function (resId, cellDataKey, rowData, rowIndex,
                             columnData, width) {
    var readOnly = !this.props.canUpdateEntry || this._isTranslationSelected(),
      placeholder = 'enter description';
    return this._renderCell({
      resId: resId,
      rowIndex: rowIndex,
      field: this.ENTRY.DESC,
      readOnly: readOnly,
      placeholder: placeholder
    });
  },

  _renderTransCountCell: function (resId, cellDataKey, rowData, rowIndex,
                              columnData, width) {
    return this._renderCell({
      resId: resId,
      rowIndex: rowIndex,
      field: this.ENTRY.TRANS_COUNT,
      readOnly: true,
      placeholder: ''
    });
  },

  _renderActionCell: function (resId, cellDataKey, rowData, rowIndex,
                            columnData, width) {
    if(resId === null) {
      return (<LoadingCell/>);
    } else if(!this.props.canUpdateEntry && !this.props.canAddNewEntry) {
      return null;
    }
    var entry = this._getGlossaryEntry(resId);
    if(this._isTranslationSelected()) {
      var info = this._generateTermInfo(entry.transTerm);
      return (
        <ActionCell info={info}
          canUpdateEntry={this.props.canUpdateEntry}
          resId={resId}
          rowIndex={rowIndex}/>
      );
    } else {
      var info = this._generateTermInfo(entry.srcTerm);
      return (
        <SourceActionCell resId={resId} rowIndex={rowIndex}
          srcLocaleId={this.props.srcLocale.locale.localeId}
          info={info}
          canUpdateEntry={this.props.canUpdateEntry}
          canDeleteEntry={this.props.canAddNewEntry}/>
      );
    }
  },

  _isTranslationSelected: function () {
    return !StringUtils.isEmptyOrNull(this.props.selectedTransLocale);
  },

  _getSourceColumn: function() {
    var srcLocaleName = "";
    if(!_.isUndefined(this.props.srcLocale) && !_.isNull(this.props.srcLocale)) {
      srcLocaleName = this.props.srcLocale.locale.displayName;
    }
    return (
      <Column
        label={srcLocaleName}
        key={this.ENTRY.SRC.field}
        width={150}
        dataKey={0}
        flexGrow={1}
        cellRenderer={this._renderSourceCell}
        headerRenderer={this._renderSourceHeader}/>
    );
  },

  _getTransColumn: function() {
    return (
      <Column
        label="Translations"
        key={this.ENTRY.TRANS.field}
        width={150}
        dataKey={0}
        flexGrow={1}
        cellRenderer={this._renderTransCell}
        headerRenderer={this._renderTransHeader}/>
    );
  },

  _getPosColumn: function() {
    return (
      <Column
        label="Part of Speech"
        key={this.ENTRY.POS.field}
        width={150}
        dataKey={0}
        cellRenderer={this._renderPosCell}
        headerRenderer={this._renderPosHeader}/>
    );
  },

  _getDescColumn: function() {
    return (
      <Column
        label="Description"
        key={this.ENTRY.DESC.field}
        width={150}
        flexGrow={1}
        dataKey={0}
        cellRenderer={this._renderDescCell}
        headerRenderer={this._renderDescHeader}/>
    );
  },

  _getTransCountColumn: function() {
    return (
      <Column
        label="Translations"
        key={this.ENTRY.TRANS_COUNT.field}
        width={120}
        cellClassName="tac"
        dataKey={0}
        cellRenderer={this._renderTransCountCell}
        headerRenderer={this._renderTransCountHeader}/>
    );
  },

  _getActionColumn: function() {
    return (
      <Column
        label=""
        key="Actions"
        cellClassName="ph1/4"
        width={300}
        dataKey={0}
        isResizable={false}
        cellRenderer={this._renderActionCell}/>
    )
  },

  _onRowMouseEnter: function (event, rowIndex) {
    if (this.state.hoveredRow !== rowIndex) {
      this.setState({hoveredRow: rowIndex});
    }
  },

  _onRowMouseLeave: function () {
    if (this.state.hoveredRow !== this.NO_ROW) {
      this.setState({hoveredRow: this.NO_ROW});
    }
  },

  _onRowClick: function (event, rowIndex) {
    var resId = this._rowGetter(rowIndex)[this.resIdIndex];
    if(this.props.focusedRow.rowIndex !== rowIndex) {
      Actions.updateFocusedRow(resId, rowIndex);
    }
  },

  _rowClassNameGetter: function (rowIndex) {
    if(this.props.focusedRow && this.props.focusedRow.rowIndex === rowIndex) {
      return 'bgcsec30a cdtrigger';
    } else if(this.state.hoveredRow === rowIndex) {
      return 'bgcsec20a cdtrigger';
    }
  },

  _getGlossaryEntry: function (resId) {
    return this.props.glossaryData[resId];
  },

  /**
   * returns resId in list for glossary entry.
   * Used for fixed-data-table when loading each row. See {@link _getGlossaryEntry}
   * @param rowIndex
   * @returns [resId] - resId in list
   */
  _rowGetter: function(rowIndex) {
    var self = this,
      row = this.props.glossaryResId[rowIndex];
    if(_.isUndefined(row) || row === null) {
      if(this.state.timeout !== null) {
        clearTimeout(self.state.timeout);
      }
      this.state.timeout = setTimeout(function() {
        Actions.loadGlossary(rowIndex);
      }, self.TIMEOUT);
      return [null];
    } else {
      return row;
    }
  },

  render: function() {
    var columns = [];

    columns.push(this._getSourceColumn());
    if(this._isTranslationSelected()) {
      columns.push(this._getTransColumn());
    } else {
      columns.push(this._getTransCountColumn());
    }
    columns.push(this._getPosColumn());
    columns.push(this._getDescColumn());
    columns.push(this._getActionColumn());

    return (
      <Table
        onRowClick={this._onRowClick}
        onRowMouseEnter={this._onRowMouseEnter}
        onRowMouseLeave={this._onRowMouseLeave}
        rowClassNameGetter={this._rowClassNameGetter}
        rowHeight={this.CELL_HEIGHT}
        rowGetter={this._rowGetter}
        rowsCount={this.props.totalCount}
        width={this.state.tbl_width}
        height={this.state.tbl_height}
        headerHeight={this.CELL_HEIGHT}>
        {columns}
      </Table>
    );
  }
});

export default DataTable;
