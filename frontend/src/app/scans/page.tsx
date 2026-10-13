"use client";

import { useQuery } from '@tanstack/react-query';
import { CheckCircle, XCircle, Clock, ChevronRight, RefreshCw, Loader2 } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import axios from 'axios';

type Scan = {
  id: string;
  projectId: string;
  branch: string;
  commitHash: string;
  status: string;
  progress: number;
  jobs: { id: string; scanner: string; status: string; findingCount: number }[];
  summary: { totalFindings: number; critical: number; high: number; medium: number; low: number };
  createdAt: string;
  completedAt: string | null;
};

type Project = { id: string; name: string };

export default function ScansPage() {
  const { data: projectsData } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/projects');
      return res.data.content as Project[];
    }
  });
  const projectMap = new Map((projectsData || []).map(p => [p.id, p.name]));

  // Fetch scans for all projects
  const { data: allScans, isLoading } = useQuery({
    queryKey: ['all_scans', projectsData],
    queryFn: async () => {
      if (!projectsData || projectsData.length === 0) return [];
      const results: Scan[] = [];
      for (const p of projectsData) {
        try {
          const res = await axios.get(`/api/v1/projects/${p.id}/scans`);
          results.push(...(res.data.content as Scan[]));
        } catch { /* skip */ }
      }
      return results.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    },
    enabled: !!projectsData && projectsData.length > 0,
    refetchInterval: 5000
  });

  const scans = allScans || [];

  const getStatusDisplay = (status: string, progress: number) => {
    if (status === 'COMPLETED') return <span className="badge badge-success flex gap-1.5"><CheckCircle className="w-3 h-3" /> Success</span>;
    if (status === 'FAILED' || status === 'ERROR') return <span className="badge badge-critical flex gap-1.5"><XCircle className="w-3 h-3" /> Failed</span>;
    if (status === 'RUNNING') return <span className="badge badge-info flex gap-1.5 animate-pulse"><RefreshCw className="w-3 h-3 animate-spin" /> {progress}%</span>;
    return <span className="badge badge-neutral">{status}</span>;
  };

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-display text-white">Execution History</h1>
          <p className="text-zinc-400 mt-1 text-sm">Traceability and logs for all automated analysis pipelines.</p>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-zinc-500" />
        </div>
      ) : scans.length === 0 ? (
        <div className="flex flex-col items-center justify-center p-12 border border-dashed border-white/10 rounded-xl bg-zinc-900/50">
          <Clock className="w-12 h-12 text-zinc-600 mb-4" />
          <h3 className="text-lg font-medium text-white mb-2">No scans yet</h3>
          <p className="text-zinc-400 text-sm">Trigger a pipeline from a project to see execution history here.</p>
        </div>
      ) : (
        <div className="sg-card overflow-hidden mt-6">
          <div className="overflow-x-auto">
            <table className="sg-table">
              <thead>
                <tr>
                  <th className="w-[120px]">Status</th>
                  <th>Target</th>
                  <th>Context</th>
                  <th>Timing</th>
                  <th>Results</th>
                  <th className="w-[50px]"></th>
                </tr>
              </thead>
              <tbody>
                {scans.map((scan) => (
                  <tr key={scan.id} className="group cursor-pointer" onClick={() => window.location.href = `/scans/${scan.id}`}>
                    <td>{getStatusDisplay(scan.status, scan.progress)}</td>
                    <td>
                      <div className="font-medium text-zinc-200 group-hover:text-indigo-400 transition-colors">
                        {projectMap.get(scan.projectId) || scan.projectId.substring(0, 8)}
                      </div>
                    </td>
                    <td>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-zinc-500">{scan.branch}</span>
                        <span className="px-1.5 py-0.5 bg-white/5 border border-white/10 rounded text-[10px] font-mono text-zinc-400">
                          {scan.commitHash ? scan.commitHash.substring(0, 7) : 'latest'}
                        </span>
                      </div>
                    </td>
                    <td>
                      <div className="flex flex-col gap-0.5 text-xs">
                        <span className="text-zinc-300 flex items-center gap-1.5">
                          <Clock className="w-3 h-3 text-zinc-500" />
                          {formatDistanceToNow(new Date(scan.createdAt), { addSuffix: true })}
                        </span>
                      </div>
                    </td>
                    <td>
                      {scan.status === 'COMPLETED' ? (
                        <span className={`text-xs font-semibold ${scan.summary.totalFindings > 0 ? 'text-red-400' : 'text-emerald-400'}`}>
                          {scan.summary.totalFindings} found
                        </span>
                      ) : (
                        <span className="text-xs text-zinc-600">—</span>
                      )}
                    </td>
                    <td className="text-right pr-4">
                      <ChevronRight className="w-4 h-4 text-zinc-600 group-hover:text-indigo-400 transition-colors inline-block" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
