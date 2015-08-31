import React from 'react';
import Configs from '../constants/Configs';
import GlossaryStore from '../stores/GlossaryStore';
import { PureRenderMixin } from 'react/addons';
import Actions from '../actions/GlossaryActions';
import { Input, Icons, Icon, Select } from 'zanata-ui'
import GlossaryDataTable from './glossary/GlossaryDataTable'
import GlossarySrcDataTable from './glossary/GlossarySrcDataTable'


var SystemGlossary = React.createClass({
  mixins: [PureRenderMixin],

  getLocaleStats: function() {
    return GlossaryStore.init();
  },

  getInitialState: function() {
    return this.getLocaleStats();
  },

  componentDidMount: function() {
    GlossaryStore.addChangeListener(this._onChange);
  },

  componentWillUnmount: function() {
    GlossaryStore.removeChangeListener(this._onChange);
  },

  _onChange: function() {
    this.setState(this.getLocaleStats());
  },

  _handleTransChange: function(localeId) {
    Actions.changeTransLocale(localeId)
  },

  render: function() {
    var contents, count = 0,
      selectedTransLocale = this.state.selectedTransLocale;

    if(this.state.glossary && _.size(this.state.glossary) > 0) {
      count = this.state.srcLocale.count;

      if(selectedTransLocale) {
        contents = (
            <GlossaryDataTable
              localeOptions={this.state.localeOptions}
              glossaryData={this.state.glossary}
              totalCount={this.state.srcLocale.count}
              canAddNewEntry={this.state.canAddNewEntry}
              canUpdateEntry={this.state.canUpdateEntry}
              isAuthenticated={Configs.authenticated}
              user={Configs.user}
              srcLocale={this.state.srcLocale}
              selectedTransLocale={selectedTransLocale}/>
          );
      } else {
        contents = (
          <GlossarySrcDataTable
            localeOptions={this.state.localeOptions}
            glossaryData={this.state.glossary}
            totalCount={this.state.srcLocale.count}
            canAddNewEntry={this.state.canAddNewEntry}
            canUpdateEntry={this.state.canUpdateEntry}
            isAuthenticated={Configs.authenticated}
            user={Configs.user}
            srcLocale={this.state.srcLocale}/>
        );
      }
    } else {
      contents = (<div>No glossary</div>)
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
                </div>
              </div>
              <div className='dfx aic mb1'>
                <div className='fxauto'>
                  <div className='posr w8'>
                    <Input name='glossarySearch' label='Search Glossary' outline className='w100p pr1&1/2' type='search' placeholder='Search Glossary' />
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
                {contents}
              </div>
            </div>);
  }
});

export default SystemGlossary;
