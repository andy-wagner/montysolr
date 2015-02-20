/**
 * Created by rchyla on 3/13/14.
 */

/*
 * This module contains a set of utilities that can be added to classes
 * to give them certain functionality
 */
define(['underscore'], function(_) {
  var Mixin = {

    /*
     * BeeHive is the object that allows modules to get access to objects
     * of the application (but we make sure these objects are protected
     * and only application can set/change them). This mixin gives objects
     * functions to query 'BeeHive'
     */
    BeeHive: {
      // called by parents (app) to give modules access
      setBeeHive: function(brundibar) {
        this.BeeHive = brundibar;
      },
      getBeeHive: function() {
        return this.BeeHive;
      },
      hasBeeHive: function() {
        if (this.BeeHive && this.BeeHive.active) {
          return true;
        }
        return false;
      }

    },

    /*
     * PubSub is the publish/subscribe queue used by our components
     * to send messages to/from.
     */
    PubSub: {
      setPubSub: function(beehive) {
        this.pubsub = beehive.getService('PubSub').getHardenedInstance();
      },
      getPubSub: function() {
        return this.pubsub;
      },
      hasPubSub: function() {
        if (this.pubsub) {
          return true;
        }
        return false;
      }
    },

    App: {
      setApp: function(app) {
        this.app = app;
      },
      getApp: function() {
        return this.app;
      },
      hasApp: function() {
        return this.app ? true : false;
      }
    }
  };

  return Mixin;
});