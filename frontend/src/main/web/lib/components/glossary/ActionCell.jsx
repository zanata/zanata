import React from 'react';
import {PureRenderMixin} from 'react/addons';
import { Button, Icon, Tooltip, OverlayTrigger } from 'zanata-ui';
import Actions from '../../actions/GlossaryActions';
import LoadingCell from './LoadingCell'
import Comment from './Comment'
import GlossaryStore from '../../stores/GlossaryStore';
import StringUtils from '../../utils/StringUtils'
import _ from 'lodash';

var ActionCell = React.createClass({
  propTypes: {
    contentHash: React.PropTypes.string.isRequired,
    info: React.PropTypes.string.isRequired,
    rowIndex: React.PropTypes.number.isRequired,
    canUpdateEntry: React.PropTypes.bool
  },

  mixins: [PureRenderMixin],

  getInitialState: function() {
    return this._getState();
  },

  _getState: function() {
    return {
      entry: GlossaryStore.getEntry(this.props.contentHash)
    }
  },

  componentDidMount: function() {
    GlossaryStore.addChangeListener(this._onChange);
  },

  componentWillUnmount: function() {
    GlossaryStore.removeChangeListener(this._onChange);
  },

  _onChange: function() {
    if (this.isMounted()) {
      this.setState(this._getState());
    }
  },

  _handleUpdate: function() {
    Actions.updateGlossary(this.props.contentHash);
  },

  _handleCancel: function() {
    Actions.resetEntry(this.props.contentHash);
  },

  _onUpdateComment: function (value) {
    Actions.updateComment(this.props.contentHash, value);
  },

  render: function () {
    var self = this;

    if (self.props.contentHash === null || self.state.entry === null) {
      return (<LoadingCell/>);
    } else {
      var isTransModified = self.state.entry.status.isTransModified;
      var canUpdateComment = self.state.entry.status.canUpdateTransComment;
      var isSaving = self.state.entry.status.isSaving === true;

      var infoTooltip = <Tooltip id="info">{self.props.info}</Tooltip>;
      var info = (
        <OverlayTrigger placement='top' rootClose overlay={infoTooltip}>
          <Icon className="cpri" name="info"/>
        </OverlayTrigger>);

      var updateButton = null,
        cancelButton = null,
        comment = (
          <Comment
            className="ml1/4"
            readOnly={!self.props.canUpdateEntry || !canUpdateComment || isSaving}
            value={self.state.entry.transTerm.comment}
            onUpdateCommentCallback={self._onUpdateComment}/>
        );

      if(isSaving) {
        return (
          <div>
            {info} {comment}
            <Button kind='primary' className="ml1/4" loading>Update</Button>
          </div>
        );
      }

      if(isTransModified) {
        updateButton = (
          <Button kind='primary' className='ml1/4' onClick={self._handleUpdate}>
            Update
          </Button>
        );
        cancelButton = (
          <Button className='ml1/4' link onClick={self._handleCancel}>
            Cancel
          </Button>
        );
      }

      return (
        <div>
          {info} {comment}
          <div className='cdtargetib'>
            {updateButton} {cancelButton}
          </div>
        </div>);
    }
  }
});

export default ActionCell;
