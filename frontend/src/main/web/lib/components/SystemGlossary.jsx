import React from 'react';
import Configs from '../constants/Configs';
import GlossaryStore from '../stores/GlossaryStore';
import { PureRenderMixin } from 'react/addons';
import Actions from '../actions/GlossaryActions';
import { Input, Icons, Icon, Select } from 'zanata-ui'
import GlossaryDataTable from './glossary/GlossaryDataTable'
import GlossarySrcDataTable from './glossary/GlossarySrcDataTable'
import TextInput from './glossary/TextInput'
import _ from 'lodash';

var SystemGlossary = React.createClass({
  mixins: [PureRenderMixin],

  _init: function() {
    return GlossaryStore.init();
  },

  getInitialState: function() {
    return this._init();
  },

  componentDidMount: function() {
    GlossaryStore.addChangeListener(this._onChange);
  },

  componentWillUnmount: function() {
    GlossaryStore.removeChangeListener(this._onChange);
  },

  _onChange: function() {
    this.setState(this._init());
  },

  _handleTransChange: function(localeId) {
    Actions.changeTransLocale(localeId)
  },

  _handleFilterKeyDown: function(event) {
    if(event.key == 'Enter') {
      Actions.updateFilter(this.state.filter);
    }
  },

  _handleFilterValueChange: function(event) {
    this.setState({filter: event.target.value});
  },

  _handleFile: function(e) {
    var self = this, reader = new FileReader();
    var file = e.target.files[0];

    reader.onload = function(upload) {
      self.setState({
        upload_file: {
          data_uri: upload.target.result,
          file:file,
          name: file.name,
          size: file.size,
          type: file.type
        }
      });
    };
    reader.readAsDataURL(file);
  },

  _uploadFile: function() {
    Actions.uploadFile(this.state.upload_file,
      this.state.srcLocale.locale.localeId,
      this.state.selectedTransLocale);
  },

  render: function() {
    var contents, count = 0,
      selectedTransLocale = this.state.selectedTransLocale,
      loadingSection = (<span></span>);

    if(this.state.loading === true) {
      loadingSection = (<span>Loading</span>);
    }

    if(selectedTransLocale) {
      contents = (
        <GlossaryDataTable
          glossaryData={this.state.glossary}
          glossaryResId={this.state.glossaryResId}
          totalCount={this.state.glossaryResId.length}
          canAddNewEntry={this.state.canAddNewEntry}
          canUpdateEntry={this.state.canUpdateEntry}
          isAuthenticated={Configs.authenticated}
          user={Configs.user}
          sort={this.state.sort}
          srcLocale={this.state.srcLocale}
          selectedTransLocale={selectedTransLocale}/>
      );
    } else {
      contents = (
        <GlossarySrcDataTable
          glossaryData={this.state.glossary}
          glossaryResId={this.state.glossaryResId}
          totalCount={this.state.glossaryResId.length}
          canAddNewEntry={this.state.canAddNewEntry}
          canUpdateEntry={this.state.canUpdateEntry}
          isAuthenticated={Configs.authenticated}
          user={Configs.user}
          sort={this.state.sort}
          srcLocale={this.state.srcLocale}/>
      );
    }


    if(this.state.srcLocale) {
      count = this.state.srcLocale.count;
    }

    return (<div>
              <Icons fileName='./node_modules/zanata-ui/src/components/Icons/icons.svg' />
              <div className='dfx aic mb1'>
                <div className='fxauto dfx aic'>
                  <h1 className='fz2 dib csec'>System Glossary</h1>
                  <Icon name='chevron-right' className='mh1/2 csec50' size='s1'/>
                  <Select
                    name='language-selection'
                    placeholder='Select a language…'
                    className='w16'
                    value={this.state.selectedTransLocale}
                    options={this.state.localeOptions}
                    onChange={this._handleTransChange}
                  />
                </div>
                <div>
                  <button className='cpri dfx aic'><Icon name='import' className='mr1/4' /><span>Import Glossary</span></button>

                  <form onSubmit={this._uploadFile} encType="multipart/form-data">
                    <input type="file" onChange={this._handleFile} ref="file" multiple={false} />
                    <input type="submit" onClick={this._uploadFile}/>
                  </form>

                </div>
              </div>
              <div className='dfx aic mb1'>
                <div className='fxauto'>
                  <div className='posr w8'>
                    <Input value={this.state.filter}
                      hideLabel
                      outline
                      label='Search Glossary'
                      placeholder="Search Glossary"
                      className="w100p pr1&1/2"
                      id="search"
                      onKeyDown={this._handleFilterKeyDown}
                      onChange={this._handleFilterValueChange} />
                    <button className='posa r0 t0 fzn1 h1&1/2 p1/4 csec50 dfx aic'>
                      <Icon name='search' size='s1' />
                    </button>
                  </div>
                </div>
                <div className='dfx aic'>
                  <Icon name='glossary' className='csec50 mr1/4' />
                  <span className='csec'>{count}</span>
                </div>
              </div>
              <div>
                {loadingSection}
                {contents}
              </div>
            </div>);
  }
});

export default SystemGlossary;
