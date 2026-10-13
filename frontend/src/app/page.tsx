"use client";

import { useQuery } from '@tanstack/react-query';
import { Activity, ShieldAlert, FolderGit2, AlertTriangle, Play, ChevronRight, Terminal, Search, Lock } from 'lucide-react';
import Link from 'next/link';
import { formatDistanceToNow } from 'date-fns';
import { motion } from 'framer-motion';

export default function DashboardPage() {
  // Mock data for Phase 1
  const stats = [
    { name: 'Monitored Projects', value: '4', icon: FolderGit2, color: 'text-indigo-400', bg: 'bg-indigo-500/10' },
    { name: 'Active Threats', value: '182', icon: ShieldAlert, color: 'text-red-400', bg: 'bg-red-500/10' },
    { name: 'Critical Fixes Required', value: '12', icon: AlertTriangle, color: 'text-orange-400', bg: 'bg-orange-500/10' },
    { name: 'Scans (24h)', value: '24', icon: Activity, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
  ];

  const recentScans = [
    { id: '1', project: 'vulnerable-demo-app', status: 'COMPLETED', findings: 45, time: new Date(Date.now() - 1000 * 60 * 30), branch: 'main' },
    { id: '2', project: 'payment-service', status: 'COMPLETED', findings: 2, time: new Date(Date.now() - 1000 * 60 * 120), branch: 'feat/stripe' },
    { id: '3', project: 'auth-gateway', status: 'FAILED', findings: 0, time: new Date(Date.now() - 1000 * 60 * 60 * 5), branch: 'main' },
  ];

  return (
    <div className="space-y-8 pb-12">
      {/* Header Area */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-display text-white flex items-center gap-3">
            <Lock className="w-8 h-8 text-indigo-500" />
            Security Overview
          </h1>
          <p className="text-zinc-400 mt-2 text-sm max-w-2xl">
            Real-time posture of your software supply chain. Monitoring 4 repositories across 2 organizations.
          </p>
        </div>
        <div className="flex gap-3">
          <button className="btn-secondary">
            <Search className="w-4 h-4" />
            Global Search
          </button>
          <button className="btn-primary">
            <Play className="w-4 h-4" />
            Trigger Scan
          </button>
        </div>
      </div>

      {/* Hero Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 stagger-children">
        {stats.map((stat) => (
          <div key={stat.name} className="sg-card p-5 group relative overflow-hidden">
            <div className={`absolute top-0 right-0 w-32 h-32 ${stat.bg} rounded-full blur-3xl -mr-10 -mt-10 opacity-50 group-hover:opacity-100 transition-opacity`} />
            <div className="flex items-start justify-between relative z-10">
              <div className="space-y-1">
                <span className="metric-label">{stat.name}</span>
                <div className="metric-value">{stat.value}</div>
              </div>
              <div className={`p-2 rounded-lg bg-zinc-800/50 border border-white/5 ${stat.color}`}>
                <stat.icon className="w-5 h-5" />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 stagger-children">
        {/* Activity Stream */}
        <div className="lg:col-span-2 sg-card flex flex-col h-[420px]">
          <div className="p-5 border-b border-white/5 flex items-center justify-between shrink-0">
            <h2 className="text-heading-2 text-white flex items-center gap-2">
              <Terminal className="w-5 h-5 text-indigo-400" />
              Recent Execution Activity
            </h2>
            <Link href="/scans" className="text-xs font-medium text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
              View all <ChevronRight className="w-3 h-3" />
            </Link>
          </div>
          
          <div className="flex-1 overflow-y-auto p-2">
            <div className="space-y-1">
              {recentScans.map((scan) => (
                <Link key={scan.id} href={`/scans/${scan.id}`} className="block group">
                  <div className="flex items-center justify-between p-3 rounded-lg hover:bg-white/5 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className="relative dot-live flex shrink-0">
                        <div className={`w-2.5 h-2.5 rounded-full ${scan.status === 'COMPLETED' ? 'bg-emerald-500' : 'bg-red-500'}`} />
                      </div>
                      <div>
                        <h3 className="text-sm font-medium text-white flex items-center gap-2">
                          {scan.project}
                          <span className="text-[10px] bg-zinc-800 text-zinc-400 px-1.5 py-0.5 rounded font-mono border border-white/5">
                            {scan.branch}
                          </span>
                        </h3>
                        <div className="text-xs text-zinc-500 mt-1">
                          {formatDistanceToNow(scan.time, { addSuffix: true })}
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <div className="text-sm font-medium text-white">{scan.findings} Findings</div>
                        <div className={`text-xs ${scan.status === 'COMPLETED' ? 'text-emerald-400' : 'text-red-400'}`}>
                          {scan.status}
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-zinc-600 group-hover:text-indigo-400 transition-colors" />
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </div>

        {/* Risk Profile */}
        <div className="sg-card p-5 h-[420px] flex flex-col">
          <h2 className="text-heading-2 text-white mb-6">Risk Profile</h2>
          
          <div className="space-y-5 flex-1">
            {[
              { label: 'Critical', count: 12, color: 'bg-red-500', width: '15%', desc: 'Immediate action required' },
              { label: 'High', count: 45, color: 'bg-orange-500', width: '45%', desc: 'Prioritize next sprint' },
              { label: 'Medium', count: 89, color: 'bg-yellow-500', width: '70%', desc: 'Schedule review' },
              { label: 'Low', count: 36, color: 'bg-indigo-500', width: '30%', desc: 'Backlog' },
            ].map((item) => (
              <div key={item.label} className="group cursor-default">
                <div className="flex justify-between items-end mb-2">
                  <div>
                    <span className="text-sm font-medium text-zinc-300 block">{item.label}</span>
                    <span className="text-[10px] text-zinc-500 uppercase tracking-wider">{item.desc}</span>
                  </div>
                  <span className="text-sm font-mono text-white">{item.count}</span>
                </div>
                <div className="progress-bar">
                  <div className={`progress-bar-fill ${item.color}`} style={{ width: item.width }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
